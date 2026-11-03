package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import io.github.strawberrymc.cutscene.MonumentCutscene;
import io.github.strawberrymc.monument.MonumentShell;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

// The first time a player enters an ocean monument, the server tells their client to play the monument cutscene
// (cutscene/MonumentCutscene). Whether they've seen it is saved on the player and kept through death.
// The chime lives in its own built-in resource pack (src/main/resources/monument_cutscene) so MCreator's
// generated sounds.json can't overwrite it.
public class MonumentIntro {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "strawberrymc");

	public static final Supplier<AttachmentType<Boolean>> SEEN_MONUMENT = ATTACHMENTS.register("seen_monument_intro",
		() -> AttachmentType.builder(() -> false).serialize(Codec.BOOL.fieldOf("seen")).copyOnDeath().build());

	// Checked every half second rather than every tick
	private static final int CHECK_INTERVAL = 10;
	// Length of the cutscene: three scenes of the monument, then back to the player
	public static final int CUTSCENE_TICKS = 425;
	// Players watching the cutscene can't see what's around them, so until this game time they take no damage and
	// mobs can't target them. The game keeps running so kelp, seagrass and everything else stays animated
	private static final Map<UUID, Long> PROTECTED_UNTIL = new HashMap<>();

	// vent is the roof gazebo's vent and sensor one of the hall's Prismatic Sensors, for the cutscene's close-ups
	public record PlayCutscene(BlockPos min, BlockPos max, Direction entrance, BlockPos vent, BlockPos sensor) implements CustomPacketPayload {
		public static final Type<PlayCutscene> TYPE = new Type<>(Identifier.fromNamespaceAndPath("strawberrymc", "monument_cutscene"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayCutscene> STREAM_CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, PlayCutscene::min, BlockPos.STREAM_CODEC, PlayCutscene::max,
				Direction.STREAM_CODEC, PlayCutscene::entrance, BlockPos.STREAM_CODEC, PlayCutscene::vent, BlockPos.STREAM_CODEC, PlayCutscene::sensor,
				PlayCutscene::new);

		@Override
		public Type<PlayCutscene> type() {
			return TYPE;
		}
	}

	public static void init(IEventBus eventBus) {
		ATTACHMENTS.register(eventBus);
		eventBus.addListener(MonumentIntro::registerPayloads);
		eventBus.addListener(MonumentIntro::addPack);
		NeoForge.EVENT_BUS.addListener(MonumentIntro::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(MonumentIntro::registerCommands);
		NeoForge.EVENT_BUS.addListener(MonumentIntro::onIncomingDamage);
		NeoForge.EVENT_BUS.addListener(MonumentIntro::onChangeTarget);
	}

	// Debug command: /cutscene clear [players] forgets that the players (default: you) have seen the cutscene, so it
	// plays again the next time they enter a monument. A player inside a monument also gets its roof vent rebuilt, so the
	// vent cutscene plays again when its Ancient Prismarine is mined
	// Whether the player is watching a monument cutscene (and so is kept safe)
	public static boolean isInCutscene(ServerPlayer player) {
		return isWatchingCutscene(player);
	}

	private static boolean isWatchingCutscene(@Nullable Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return false;
		Long until = PROTECTED_UNTIL.get(player.getUUID());
		if (until == null)
			return false;
		if (player.level().getGameTime() < until)
			return true;
		PROTECTED_UNTIL.remove(player.getUUID());
		return false;
	}

	// Keeps a player safe from damage and targeting for the next ticks, while they watch a cutscene
	public static void protect(ServerPlayer player, int ticks) {
		PROTECTED_UNTIL.merge(player.getUUID(), player.level().getGameTime() + ticks, Math::max);
	}

	private static void onIncomingDamage(LivingIncomingDamageEvent event) {
		// /kill and the void still work
		if (isWatchingCutscene(event.getEntity()) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			event.setCanceled(true);
	}

	private static void onChangeTarget(LivingChangeTargetEvent event) {
		if (isWatchingCutscene(event.getNewAboutToBeSetTarget()))
			event.setCanceled(true);
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("cutscene")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("clear")
				.executes(context -> clear(context.getSource(), List.of(context.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
	}

	private static int clear(CommandSourceStack source, Collection<ServerPlayer> players) {
		int vents = 0;
		for (ServerPlayer player : players) {
			player.setData(SEEN_MONUMENT, false);
			io.github.strawberrymc.monument.MonumentQuest.setStage(player, io.github.strawberrymc.monument.MonumentQuest.Stage.NONE, null);
			// The vent cutscene plays once per monument, so rebuild the vent (and the arena's rune) of the monument the player is in
			if (io.github.strawberrymc.monument.PrismaticSensors.resetVent(player.level(), player.blockPosition()))
				vents++;
		}
		String reset = vents == 0 ? "" : vents == 1 ? " and reset the monument's vent and arena" : " and reset " + vents + " monuments' vents and arenas";
		source.sendSuccess(() -> players.size() == 1
			? Component.literal("Cleared cutscene(s) for " + players.iterator().next().getName().getString() + reset)
			: Component.literal("Cleared cutscene(s) for " + players.size() + " players" + reset), true);
		return players.size();
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		// The handler only runs on the client, so the client-only cutscene class is never loaded on a server
		event.registrar("3").playToClient(PlayCutscene.TYPE, PlayCutscene.STREAM_CODEC,
			(payload, context) -> MonumentCutscene.start(payload.min(), payload.max(), payload.entrance(), payload.vent(), payload.sensor()));
	}

	private static void addPack(AddPackFindersEvent event) {
		event.addPackFinders(Identifier.fromNamespaceAndPath("strawberrymc", "monument_cutscene"), PackType.CLIENT_RESOURCES,
			Component.literal("Strawberry MC monument cutscene"), PackSource.BUILT_IN, true, Pack.Position.TOP);
	}

	private static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % CHECK_INTERVAL != 0 || player.isSpectator()
				|| player.getData(SEEN_MONUMENT))
			return;
		ServerLevel level = (ServerLevel) player.level();
		StructureStart monument = level.structureManager().getStructureWithPieceAt(player.blockPosition(),
			structure -> structure.is(BuiltinStructures.OCEAN_MONUMENT));
		if (!monument.isValid() || monument.getPieces().isEmpty())
			return;
		// The first piece is the monument building; the start's own box is widened for MonumentBasinMixin's basin
		StructurePiece building = monument.getPieces().get(0);
		BoundingBox box = building.getBoundingBox();
		// The entrance is on the building's local front (z=0), which is the side opposite its orientation
		Direction entrance = building.getOrientation() == null ? Direction.SOUTH : building.getOrientation().getOpposite();
		MonumentShell.Layout layout = MonumentShell.layout(building);
		// The sensor nearest the player, whose chunks are sure to be loaded on their client
		BlockPos sensor = layout.sensors().stream().min(java.util.Comparator.comparingDouble(pos -> pos.distSqr(player.blockPosition()))).orElseThrow();
		player.setData(SEEN_MONUMENT, true);
		PROTECTED_UNTIL.put(player.getUUID(), level.getGameTime() + CUTSCENE_TICKS);
		// Guardians already attacking the player stop; onChangeTarget keeps them (and anything else) from starting again
		for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), mob -> mob.getTarget() == player))
			mob.setTarget(null);
		PacketDistributor.sendToPlayer(player, new PlayCutscene(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()), entrance,
			layout.ventCenter(), sensor));
	}
}

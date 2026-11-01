package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import io.github.strawberrymc.cutscene.MonumentCutscene;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Collection;
import java.util.List;
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

	public record PlayCutscene(BlockPos min, BlockPos max) implements CustomPacketPayload {
		public static final Type<PlayCutscene> TYPE = new Type<>(Identifier.fromNamespaceAndPath("strawberrymc", "monument_cutscene"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayCutscene> STREAM_CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, PlayCutscene::min, BlockPos.STREAM_CODEC, PlayCutscene::max, PlayCutscene::new);

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
	}

	// Debug command: /cutscene clear [players] forgets that the players (default: you) have seen the cutscene, so it
	// plays again the next time they enter a monument
	private static void registerCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("cutscene")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("clear")
				.executes(context -> clear(context.getSource(), List.of(context.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(context -> clear(context.getSource(), EntityArgument.getPlayers(context, "targets"))))));
	}

	private static int clear(CommandSourceStack source, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players)
			player.setData(SEEN_MONUMENT, false);
		source.sendSuccess(() -> players.size() == 1
			? Component.literal("Cleared cutscene(s) for " + players.iterator().next().getName().getString())
			: Component.literal("Cleared cutscene(s) for " + players.size() + " players"), true);
		return players.size();
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		// The handler only runs on the client, so the client-only cutscene class is never loaded on a server
		event.registrar("1").playToClient(PlayCutscene.TYPE, PlayCutscene.STREAM_CODEC,
			(payload, context) -> MonumentCutscene.start(payload.min(), payload.max()));
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
		BoundingBox box = monument.getPieces().get(0).getBoundingBox();
		player.setData(SEEN_MONUMENT, true);
		PacketDistributor.sendToPlayer(player, new PlayCutscene(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ())));
	}
}

package io.github.strawberrymc.monument;

import io.github.strawberrymc.MonumentIntro;
import io.github.strawberrymc.init.StrawberrymcModBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

// The Prismatic Sensors guarding each expanded Ocean Monument (see MonumentShell). While a quadrant's sensor is active,
// survival players can't break or place blocks in that quarter of the monument, except Ancient Prismarine. Sensors
// switch off once the water dragon is beaten; until it exists, /sensors activate|deactivate switches them.
//
// Mining the Ancient Prismarine capping the roof vent plays the vent cutscene for everyone nearby: a jet of water
// rises out of the vent and shatters the Cracked Prismarine dome hanging over it.
public class PrismaticSensors {
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "strawberrymc");
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SENSOR_GLYPH = PARTICLES.register("sensor_glyph", () -> new SimpleParticleType(false));

	private static final int CUTSCENE_RANGE = 48;
	// When the jet leaves the vent and when it hits the dome, in ticks after the cap is mined. The cutscene is timed to match
	public static final int JET_START = 30;
	public static final int JET_HIT = 70;
	public static final int VENT_CUTSCENE_TICKS = 120;
	// Vents whose jet is already running, so mining another cap block doesn't start it again
	private static final Set<BlockPos> ERUPTING = new HashSet<>();

	public record PlayVentCutscene(BlockPos vent, BlockPos lump) implements CustomPacketPayload {
		public static final Type<PlayVentCutscene> TYPE = new Type<>(Identifier.fromNamespaceAndPath("strawberrymc", "vent_cutscene"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayVentCutscene> STREAM_CODEC =
			StreamCodec.composite(BlockPos.STREAM_CODEC, PlayVentCutscene::vent, BlockPos.STREAM_CODEC, PlayVentCutscene::lump, PlayVentCutscene::new);

		@Override
		public Type<PlayVentCutscene> type() {
			return TYPE;
		}
	}

	public static void init(IEventBus eventBus) {
		PARTICLES.register(eventBus);
		eventBus.addListener(PrismaticSensors::registerPayloads);
		NeoForge.EVENT_BUS.addListener(PrismaticSensors::onBreak);
		NeoForge.EVENT_BUS.addListener(PrismaticSensors::onPlace);
		NeoForge.EVENT_BUS.addListener(PrismaticSensors::registerCommands);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		// The handler only runs on the client, so the client-only cutscene class is never loaded on a server
		event.registrar("1").playToClient(PlayVentCutscene.TYPE, PlayVentCutscene.STREAM_CODEC,
			(payload, context) -> io.github.strawberrymc.monument.client.VentCutscene.start(payload.vent(), payload.lump()));
	}

	// The expanded monument at pos, if there is one
	public static @Nullable MonumentShell.Layout monumentAt(ServerLevel level, BlockPos pos) {
		StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure -> structure.is(BuiltinStructures.OCEAN_MONUMENT));
		if (!start.isValid() || start.getPieces().isEmpty())
			return null;
		MonumentShell.Layout layout = MonumentShell.layout(start.getPieces().getFirst());
		return layout.contains(pos) ? layout : null;
	}

	// The active sensor guarding pos, or null when pos isn't protected
	public static @Nullable BlockPos guardingSensor(ServerLevel level, BlockPos pos) {
		MonumentShell.Layout layout = monumentAt(level, pos);
		if (layout == null)
			return null;
		BlockPos sensor = layout.sensorFor(pos);
		return level.isLoaded(sensor) && level.getBlockState(sensor).is(StrawberrymcModBlocks.PRISMATIC_SENSOR.get()) ? sensor : null;
	}

	private static boolean isExempt(@Nullable Player player, BlockState state) {
		return player == null || player.isCreative() || player.isSpectator() || state.is(StrawberrymcModBlocks.ANCIENT_PRISMARINE.get());
	}

	private static void onBreak(BreakBlockEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockPos pos = event.getPos();
		if (!isExempt(event.getPlayer(), event.getState())) {
			BlockPos sensor = guardingSensor(level, pos);
			if (sensor != null) {
				event.setCanceled(true);
				refuse(level, sensor, pos);
			}
			return;
		}
		if (event.getState().is(StrawberrymcModBlocks.ANCIENT_PRISMARINE.get()))
			checkVent(level, pos);
	}

	private static void onPlace(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player))
			return;
		if (isExempt(player, event.getPlacedBlock()))
			return;
		BlockPos sensor = guardingSensor(level, event.getPos());
		if (sensor != null) {
			event.setCanceled(true);
			refuse(level, sensor, event.getPos());
		}
	}

	// Glyphs swirl in at the refused block, and the sensor hums
	private static void refuse(ServerLevel level, BlockPos sensor, BlockPos pos) {
		level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.4, 0.4, 0.4, 1.0);
		level.playSound(null, pos, SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 0.6F, 1.6F);
	}

	// ---- The vent ----

	private static void checkVent(ServerLevel level, BlockPos pos) {
		MonumentShell.Layout layout = monumentAt(level, pos);
		if (layout == null || !layout.ventCap().contains(pos) || ERUPTING.contains(layout.ventCenter()))
			return;
		// Only once: after the jet the dome is gone
		if (!level.getBlockState(layout.lumpCenter()).is(StrawberrymcModBlocks.CRACKED_PRISMARINE.get()))
			return;
		BlockPos vent = layout.ventCenter();
		ERUPTING.add(vent);
		for (ServerPlayer player : level.players())
			if (player.blockPosition().closerThan(vent, CUTSCENE_RANGE)) {
				PacketDistributor.sendToPlayer(player, new PlayVentCutscene(vent, layout.lumpCenter()));
				MonumentIntro.protect(player, VENT_CUTSCENE_TICKS);
			}
		level.playSound(null, vent, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 2.0F, 0.6F);
		for (int t = 0; t <= JET_HIT - JET_START + 20; t += 2) {
			int tick = t;
			io.github.strawberrymc.StrawberrymcMod.queueServerWork(JET_START + t, () -> jet(level, layout, tick));
		}
		io.github.strawberrymc.StrawberrymcMod.queueServerWork(JET_HIT, () -> shatter(level, layout));
		io.github.strawberrymc.StrawberrymcMod.queueServerWork(VENT_CUTSCENE_TICKS, () -> ERUPTING.remove(vent));
	}

	// The jet climbs from the vent to the dome, then keeps bubbling for a while after
	private static void jet(ServerLevel level, MonumentShell.Layout layout, int tick) {
		BlockPos vent = layout.ventCenter();
		double rise = Math.min(1.0, tick / (double) (JET_HIT - JET_START)) * (layout.domeTip().getY() - vent.getY());
		double x = vent.getX() + 0.5, z = vent.getZ() + 0.5;
		for (double y = 0; y <= rise; y += 0.5) {
			level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, x, vent.getY() + 0.5 + y, z, 6, 0.6, 0.2, 0.6, 0.1);
			level.sendParticles(ParticleTypes.CLOUD, x, vent.getY() + 0.5 + y, z, 2, 0.4, 0.2, 0.4, 0.02);
		}
		level.sendParticles(ParticleTypes.BUBBLE_POP, x, vent.getY() + 0.5 + rise, z, 10, 0.7, 0.3, 0.7, 0.05);
	}

	private static void shatter(ServerLevel level, MonumentShell.Layout layout) {
		for (BlockPos pos : layout.lump())
			if (level.getBlockState(pos).is(StrawberrymcModBlocks.CRACKED_PRISMARINE.get()))
				level.destroyBlock(pos, false);
		BlockPos lump = layout.lumpCenter();
		level.sendParticles(ParticleTypes.CLOUD, lump.getX() + 0.5, lump.getY() + 0.5, lump.getZ() + 0.5, 60, 1.2, 1.2, 1.2, 0.15);
		level.playSound(null, lump, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1.5F, 1.4F);
	}

	// Rebuilds the vent and the Cracked Prismarine dome of the monument pos is in, so the vent cutscene can play again.
	// Returns false when pos isn't in an expanded monument
	public static boolean resetVent(ServerLevel level, BlockPos pos) {
		MonumentShell.Layout layout = monumentAt(level, pos);
		if (layout == null)
			return false;
		ERUPTING.remove(layout.ventCenter());
		for (BlockPos cap : layout.ventCap()) {
			for (int depth = 1; depth <= 3; depth++)
				level.setBlock(cap.below(depth), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
			level.setBlock(cap, StrawberrymcModBlocks.ANCIENT_PRISMARINE.get().defaultBlockState(), Block.UPDATE_ALL);
		}
		for (BlockPos lump : layout.lump())
			level.setBlock(lump, StrawberrymcModBlocks.CRACKED_PRISMARINE.get().defaultBlockState(), Block.UPDATE_ALL);
		MonumentArena.reset(level, layout);
		return true;
	}

	// ---- Debug command ----

	private static void registerCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("sensors")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("activate").executes(context -> setSensors(context.getSource(), true)))
			.then(Commands.literal("deactivate").executes(context -> setSensors(context.getSource(), false))));
	}

	// Switches the four sensors of the monument the command is run in
	private static int setSensors(CommandSourceStack source, boolean active) {
		ServerLevel level = source.getLevel();
		MonumentShell.Layout layout = monumentAt(level, BlockPos.containing(source.getPosition()));
		if (layout == null) {
			source.sendFailure(Component.literal("Stand inside an ocean monument to switch its sensors"));
			return 0;
		}
		Block block = active ? StrawberrymcModBlocks.PRISMATIC_SENSOR.get() : StrawberrymcModBlocks.DEACTIVATED_PRISMATIC_SENSOR.get();
		int switched = 0;
		for (BlockPos sensor : layout.sensors())
			if (level.isLoaded(sensor)) {
				level.setBlock(sensor, block.defaultBlockState(), Block.UPDATE_ALL);
				switched++;
			}
		int count = switched;
		source.sendSuccess(() -> Component.literal((active ? "Activated " : "Deactivated ") + count + " Prismatic Sensor(s)"), true);
		return count;
	}
}

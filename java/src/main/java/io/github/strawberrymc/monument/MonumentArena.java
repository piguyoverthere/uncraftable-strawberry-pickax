package io.github.strawberrymc.monument;

import io.github.strawberrymc.StrawberrymcMod;
import io.github.strawberrymc.init.StrawberrymcModBlocks;
import io.github.strawberrymc.init.StrawberrymcModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The boss arena on the monument's roof (built by MonumentShell). Putting a Monument Key into the Prismatic Vault in the
// middle of the glass opens the tank: rune glyphs funnel down from the vault to the Prismatic Rune in its item frame at
// the bottom, the rune starts to glow, then breaks loose and rises to just under the glass. Everyone nearby watches it
// from inside the tank (ArenaCutscene). That's where the water dragon
// fight will start; for now the rune stays floating there.
public class MonumentArena {
	public static final String RUNE_FRAME_TAG = "strawberrymc.rune_frame";
	private static final String RISING_RUNE_TAG = "strawberrymc.rising_rune";
	public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, "strawberrymc");
	// Rune glyphs spiralling from where they spawn down into the point given as their velocity
	public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUNE_FUNNEL = PARTICLES.register("rune_funnel", () -> new SimpleParticleType(true));

	// Ticks after the key goes in: the funnel runs until GLOW, the rune breaks loose at RELEASE and rises for RISE ticks.
	// ArenaCutscene is timed to match
	public static final int GLOW = 70;
	public static final int RELEASE = 110;
	public static final int RISE = 90;
	public static final int CUTSCENE_TICKS = RELEASE + RISE + 30;
	private static final int CUTSCENE_RANGE = 48;

	// Sent to everyone nearby when the vault opens: where the rune's frame and the vault are
	public record PlayArenaCutscene(BlockPos frame, BlockPos vault) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
		public static final Type<PlayArenaCutscene> TYPE = new Type<>(net.minecraft.resources.Identifier.fromNamespaceAndPath("strawberrymc", "arena_cutscene"));
		public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, PlayArenaCutscene> STREAM_CODEC =
			net.minecraft.network.codec.StreamCodec.composite(BlockPos.STREAM_CODEC, PlayArenaCutscene::frame, BlockPos.STREAM_CODEC, PlayArenaCutscene::vault,
				PlayArenaCutscene::new);

		@Override
		public Type<PlayArenaCutscene> type() {
			return TYPE;
		}
	}
	private static final Set<BlockPos> OPENING = new HashSet<>();

	public static void init(IEventBus eventBus) {
		PARTICLES.register(eventBus);
		eventBus.addListener(MonumentArena::registerPayloads);
		NeoForge.EVENT_BUS.addListener(MonumentArena::onUseBlock);
		NeoForge.EVENT_BUS.addListener(MonumentArena::onInteractEntity);
		NeoForge.EVENT_BUS.addListener(MonumentArena::onAttackEntity);
	}

	private static void registerPayloads(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
		// The handler only runs on the client, so the client-only cutscene class is never loaded on a server
		event.registrar("1").playToClient(PlayArenaCutscene.TYPE, PlayArenaCutscene.STREAM_CODEC,
			(payload, context) -> io.github.strawberrymc.monument.client.ArenaCutscene.start(payload.frame(), payload.vault()));
	}

	private static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !level.getBlockState(event.getPos()).is(StrawberrymcModBlocks.PRISMATIC_VAULT.get()))
			return;
		ServerPlayer player = (ServerPlayer) event.getEntity();
		event.setCanceled(true);
		event.setCancellationResult(InteractionResult.SUCCESS);
		MonumentShell.Layout layout = PrismaticSensors.monumentAt(level, event.getPos());
		if (layout == null || !layout.arenaCenter().equals(event.getPos()))
			return;
		ItemFrame frame = runeFrame(level, layout);
		if (frame == null || frame.getItem().isEmpty() || OPENING.contains(layout.arenaCenter())) {
			player.sendOverlayMessage(Component.literal("The vault is already open"));
			return;
		}
		ItemStack held = player.getItemInHand(event.getHand());
		if (!held.is(StrawberrymcModItems.MONUMENT_KEY.get())) {
			player.sendOverlayMessage(Component.literal("The vault needs a Monument Key"));
			return;
		}
		if (!player.isCreative())
			held.shrink(1);
		open(level, layout);
	}

	private static void open(ServerLevel level, MonumentShell.Layout layout) {
		BlockPos vault = layout.arenaCenter();
		OPENING.add(vault);
		// Everyone nearby watches the rune wake up from inside the tank
		for (ServerPlayer player : level.players())
			if (player.blockPosition().closerThan(vault, CUTSCENE_RANGE)) {
				net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new PlayArenaCutscene(layout.runeFrame(), vault));
				io.github.strawberrymc.MonumentIntro.protect(player, CUTSCENE_TICKS);
			}
		level.playSound(null, vault, SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
		level.playSound(null, vault, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.3F);
		Vec3 target = Vec3.atCenterOf(layout.runeFrame()).subtract(0, 0.35, 0);
		for (int t = 0; t < GLOW; t += 2) {
			int tick = t;
			StrawberrymcMod.queueServerWork(t, () -> funnel(level, vault, target, tick));
		}
		StrawberrymcMod.queueServerWork(GLOW, () -> {
			ItemFrame frame = runeFrame(level, layout);
			if (frame != null)
				frame.setGlowingTag(true);
			level.sendParticles(ParticleTypes.END_ROD, target.x, target.y + 0.3, target.z, 30, 0.4, 0.3, 0.4, 0.03);
			level.playSound(null, layout.runeFrame(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2.0F, 1.0F);
		});
		StrawberrymcMod.queueServerWork(RELEASE, () -> release(level, layout));
		StrawberrymcMod.queueServerWork(RELEASE + RISE + 5, () -> OPENING.remove(vault));
	}

	// Glyphs start in a ring around the vault and spiral down into the rune
	private static void funnel(ServerLevel level, BlockPos vault, Vec3 target, int tick) {
		int count = 3 + tick / 12;
		for (int i = 0; i < count; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2, radius = 1.2 + level.getRandom().nextDouble() * 1.6;
			double x = vault.getX() + 0.5 + Math.cos(angle) * radius, z = vault.getZ() + 0.5 + Math.sin(angle) * radius;
			level.sendParticles(RUNE_FUNNEL.get(), true, true, x, vault.getY() + 1.2 + level.getRandom().nextDouble(), z, 0, target.x, target.y, target.z, 1.0);
		}
	}

	private static void release(ServerLevel level, MonumentShell.Layout layout) {
		ItemFrame frame = runeFrame(level, layout);
		if (frame == null || frame.getItem().isEmpty())
			return;
		ItemStack rune = frame.getItem().copy();
		frame.setItem(ItemStack.EMPTY, false);
		frame.setGlowingTag(false);
		Vec3 start = Vec3.atCenterOf(layout.runeFrame());
		ItemEntity floating = new ItemEntity(level, start.x, start.y, start.z, rune, 0, 0, 0);
		floating.setNoGravity(true);
		floating.setNeverPickUp();
		floating.setUnlimitedLifetime();
		floating.setGlowingTag(true);
		floating.addTag(RISING_RUNE_TAG);
		level.addFreshEntity(floating);
		level.playSound(null, layout.runeFrame(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.BLOCKS, 1.5F, 1.2F);
		// Up to just under the glass
		double topY = layout.arenaCenter().getY() - 0.6;
		for (int t = 1; t <= RISE; t++) {
			double y = start.y + (topY - start.y) * ease(t / (double) RISE);
			StrawberrymcMod.queueServerWork(t, () -> {
				if (floating.isAlive()) {
					floating.setPos(start.x, y, start.z);
					floating.setDeltaMovement(Vec3.ZERO);
					level.sendParticles(ParticleTypes.BUBBLE, start.x, y - 0.3, start.z, 2, 0.15, 0.1, 0.15, 0.02);
				}
			});
		}
	}

	private static double ease(double t) {
		return t * t * (3 - 2 * t);
	}

	static @Nullable ItemFrame runeFrame(ServerLevel level, MonumentShell.Layout layout) {
		List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, new AABB(layout.runeFrame()).inflate(1),
			frame -> frame.entityTags().contains(RUNE_FRAME_TAG));
		return frames.isEmpty() ? null : frames.getFirst();
	}

	// Puts the rune back in its frame, for /cutscene clear. Returns false when the frame is missing or its chunk isn't loaded
	public static boolean reset(ServerLevel level, MonumentShell.Layout layout) {
		OPENING.remove(layout.arenaCenter());
		for (ItemEntity rune : level.getEntitiesOfClass(ItemEntity.class, new AABB(layout.arenaCenter()).inflate(MonumentShell.Layout.ARENA_RADIUS, 12,
				MonumentShell.Layout.ARENA_RADIUS), entity -> entity.entityTags().contains(RISING_RUNE_TAG)))
			rune.discard();
		ItemFrame frame = runeFrame(level, layout);
		if (frame == null)
			return false;
		frame.setItem(new ItemStack(StrawberrymcModItems.PRISMATIC_RUNE.get()), false);
		frame.setGlowingTag(false);
		return true;
	}

	// The rune frame is part of the monument: it can't be emptied, turned or broken
	private static boolean isRuneFrame(Entity entity) {
		return entity instanceof ItemFrame && entity.entityTags().contains(RUNE_FRAME_TAG);
	}

	private static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
		if (isRuneFrame(event.getTarget()) && !event.getEntity().isCreative()) {
			event.setCanceled(true);
			event.setCancellationResult(InteractionResult.FAIL);
		}
	}

	private static void onAttackEntity(AttackEntityEvent event) {
		if (isRuneFrame(event.getTarget()) && !event.getEntity().isCreative())
			event.setCanceled(true);
	}
}

package io.github.strawberrymc.monument.client;

import io.github.strawberrymc.init.StrawberrymcModBlocks;
import io.github.strawberrymc.monument.PrismaticSensors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

import java.util.ArrayList;
import java.util.List;

// Active Prismatic Sensors throw enchanting glyphs out of the two light-blue panels on each side. The glyphs hover for a
// moment, then get sucked back into the gap between the panels. Deactivated sensors stay still.
//
// Random block ticks would reach a single sensor only every few seconds, so instead the sensors near the player are
// found every second (skipping chunk sections that can't contain one) and each spawns glyphs every tick.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class SensorParticles {
	private static final int SCAN_RADIUS_CHUNKS = 3;
	private static final int SCAN_RADIUS_SECTIONS = 3;
	private static final float GLYPHS_PER_TICK = 0.6F;
	// Where the panels are across a side, from the texture: columns 3-5 and 10-12 of 16, rows 2-13
	private static final double[] PANEL_CENTERS = {4.5 / 16, 11.5 / 16};
	private static final double PANEL_HALF_WIDTH = 1.2 / 16;
	private static final double PANEL_BOTTOM = 2.5 / 16, PANEL_TOP = 13.5 / 16;

	private static final List<BlockPos> SENSORS = new ArrayList<>();
	private static int ticks;

	@SubscribeEvent
	public static void registerProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(PrismaticSensors.SENSOR_GLYPH.get(), Glyph.Provider::new);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null || minecraft.player == null || minecraft.isPaused()) {
			SENSORS.clear();
			return;
		}
		if (ticks++ % 20 == 0)
			scan(level, minecraft.player.blockPosition());
		RandomSource random = level.getRandom();
		Block sensor = StrawberrymcModBlocks.PRISMATIC_SENSOR.get();
		for (BlockPos pos : SENSORS) {
			if (!level.getBlockState(pos).is(sensor))
				continue;
			int count = (int) GLYPHS_PER_TICK + (random.nextFloat() < GLYPHS_PER_TICK % 1 ? 1 : 0);
			for (int i = 0; i < count; i++)
				spawn(level, pos, random);
		}
	}

	private static void scan(ClientLevel level, BlockPos center) {
		SENSORS.clear();
		Block sensor = StrawberrymcModBlocks.PRISMATIC_SENSOR.get();
		int chunkX = SectionPos.blockToSectionCoord(center.getX()), chunkZ = SectionPos.blockToSectionCoord(center.getZ());
		int sectionY = SectionPos.blockToSectionCoord(center.getY());
		for (int cx = chunkX - SCAN_RADIUS_CHUNKS; cx <= chunkX + SCAN_RADIUS_CHUNKS; cx++)
			for (int cz = chunkZ - SCAN_RADIUS_CHUNKS; cz <= chunkZ + SCAN_RADIUS_CHUNKS; cz++) {
				LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, false);
				if (chunk == null)
					continue;
				for (int sy = sectionY - SCAN_RADIUS_SECTIONS; sy <= sectionY + SCAN_RADIUS_SECTIONS; sy++) {
					int index = level.getSectionIndexFromSectionY(sy);
					if (index < 0 || index >= chunk.getSectionsCount())
						continue;
					LevelChunkSection section = chunk.getSection(index);
					if (section.hasOnlyAir() || !section.getStates().maybeHas(state -> state.is(sensor)))
						continue;
					for (int x = 0; x < 16; x++)
						for (int y = 0; y < 16; y++)
							for (int z = 0; z < 16; z++)
								if (section.getBlockState(x, y, z).is(sensor))
									SENSORS.add(new BlockPos(SectionPos.sectionToBlockCoord(cx) + x, SectionPos.sectionToBlockCoord(sy) + y,
										SectionPos.sectionToBlockCoord(cz) + z));
				}
			}
	}

	// One glyph from a random panel on a random side
	private static void spawn(ClientLevel level, BlockPos pos, RandomSource random) {
		Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		// Across the side from left to right, as seen from outside
		double across = PANEL_CENTERS[random.nextInt(2)] + (random.nextDouble() * 2 - 1) * PANEL_HALF_WIDTH;
		double up = PANEL_BOTTOM + random.nextDouble() * (PANEL_TOP - PANEL_BOTTOM);
		double nx = side.getStepX(), nz = side.getStepZ();
		// Tangent along the side; the glyph's gap target is (0.5 - across) along it
		double tx = -nz, tz = nx;
		double x = pos.getX() + 0.5 + nx * 0.52 + tx * (across - 0.5);
		double z = pos.getZ() + 0.5 + nz * 0.52 + tz * (across - 0.5);
		level.addParticle(PrismaticSensors.SENSOR_GLYPH.get(), x, pos.getY() + up, z, nx, 0.5 - across, nz);
	}

	// A glyph that leaves its panel, hovers, then returns to the gap between the panels. xd and zd carry the side's
	// outward direction and yd how far along the side the gap is
	public static class Glyph extends SingleQuadParticle {
		private static final float OUT_END = 0.25F, HOVER_END = 0.62F;
		private final double startX, startY, startZ;
		private final double outX, outZ;
		private final double gapX, gapZ;
		private final double driftX, driftY, driftZ;
		private final float size;

		Glyph(ClientLevel level, double x, double y, double z, double nx, double gap, double nz, TextureAtlasSprite sprite) {
			super(level, x, y, z, sprite);
			this.startX = x;
			this.startY = y;
			this.startZ = z;
			double distance = 0.45 + random.nextDouble() * 0.55;
			this.outX = nx * distance;
			this.outZ = nz * distance;
			// The gap is along the side's tangent from the panel
			this.gapX = -nz * gap;
			this.gapZ = nx * gap;
			this.driftX = (random.nextDouble() - 0.5) * 0.35 * Math.abs(nz);
			this.driftZ = (random.nextDouble() - 0.5) * 0.35 * Math.abs(nx);
			this.driftY = (random.nextDouble() - 0.5) * 0.3;
			this.size = 0.06F + random.nextFloat() * 0.05F;
			this.quadSize = size;
			this.lifetime = 60 + random.nextInt(25);
			this.hasPhysics = false;
			this.gravity = 0;
			float brightness = 0.85F + random.nextFloat() * 0.15F;
			this.rCol = 0.55F * brightness;
			this.gCol = 0.9F * brightness;
			this.bCol = brightness;
			this.setAlpha(0);
		}

		@Override
		public SingleQuadParticle.Layer getLayer() {
			return SingleQuadParticle.Layer.TRANSLUCENT;
		}

		@Override
		public int getLightCoords(float a) {
			return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			if (age++ >= lifetime) {
				remove();
				return;
			}
			float t = (float) age / lifetime;
			double hoverX = startX + outX + driftX, hoverY = startY + driftY, hoverZ = startZ + outZ + driftZ;
			if (t < OUT_END) {
				double p = ease(t / OUT_END);
				x = Mth.lerp(p, startX, hoverX);
				y = Mth.lerp(p, startY, hoverY);
				z = Mth.lerp(p, startZ, hoverZ);
			} else if (t < HOVER_END) {
				x = hoverX;
				y = hoverY + Math.sin(age * 0.25) * 0.03;
				z = hoverZ;
			} else {
				// Sucked in: slow at first, then fast
				double p = Math.pow((t - HOVER_END) / (1 - HOVER_END), 2);
				x = Mth.lerp(p, hoverX, startX + gapX);
				y = Mth.lerp(p, hoverY, startY);
				z = Mth.lerp(p, hoverZ, startZ + gapZ);
			}
			setAlpha(Math.min(1, Math.min(t / 0.1F, (1 - t) / 0.08F)));
			quadSize = size * (t > 0.9F ? (1 - t) / 0.1F : 1);
		}

		private static double ease(double t) {
			return 1 - Math.pow(1 - t, 3);
		}

		public static class Provider implements ParticleProvider<SimpleParticleType> {
			private final SpriteSet sprites;

			public Provider(SpriteSet sprites) {
				this.sprites = sprites;
			}

			@Override
			public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double nx, double gap, double nz,
					RandomSource random) {
				return new Glyph(level, x, y, z, nx, gap, nz, sprites.get(random));
			}
		}
	}
}

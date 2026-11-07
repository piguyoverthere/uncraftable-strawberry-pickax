package io.github.strawberrymc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.strawberrymc.init.StrawberrymcModBlocks;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

// Hollows out the Crystal Hollows biome into one vast cavern: 3D noise opens huge chambers between y=-50 and y=36,
// and a 2D noise leaves stone pillars standing from floor to ceiling, thicker where they meet the rock. Every surface
// it opens is crusted over: Crystalline Calcite on the floors, calcite on the walls, ceilings and pillars.
//
// This is a carver rather than a feature so every neighbouring chunk is already carved before any feature runs; the
// crystal columns, calcite mountains, geodes and pools (MCreator procedures and features) then never get cut in half
// by a neighbour carving later. MCreator has no noise blocks, which is why this part is Java.
// Added to every biome by biome_modifier/crystal_hollows_carver.json (a chunk takes its carvers from its biome at
// y=0, which is often Crystal Hollows itself, and mod biomes aren't in #is_overworld); it only carves where the biome
// at that block is Crystal Hollows.
public class CrystalHollowsCarver extends WorldCarver<CarverConfiguration> {
	public static final DeferredRegister<WorldCarver<?>> CARVERS = DeferredRegister.create(Registries.CARVER, "strawberrymc");

	public static final Supplier<CrystalHollowsCarver> CRYSTAL_HOLLOWS =
		CARVERS.register("crystal_hollows", () -> new CrystalHollowsCarver(CarverConfiguration.CODEC.codec()));

	private static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("strawberrymc", "crystal_hollows"));
	// Kept above the deep lava level (-54) so the cavern floor doesn't open onto lava
	private static final int FLOOR = -50;
	private static final int CEILING = 36;
	// Rock left between the cavern roof and the surface or sea floor
	private static final int ROOF = 12;
	// How far the floor and ceiling curve in, as a share of the cavern height
	private static final double SHELL = 0.2;
	// Cave noise is sampled every CELL blocks and interpolated
	private static final int CELL = 4;

	private record Noises(NormalNoise cave, NormalNoise pillars, NormalNoise wobble) {
	}

	private static final Map<RandomState, Noises> NOISES = Collections.synchronizedMap(new WeakHashMap<>());

	public CrystalHollowsCarver(com.mojang.serialization.Codec<CarverConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean isStartChunk(CarverConfiguration configuration, RandomSource random) {
		return true;
	}

	@Override
	public boolean carve(CarvingContext context, CarverConfiguration configuration, ChunkAccess chunk, Function<BlockPos, Holder<Biome>> biomeGetter,
			RandomSource random, Aquifer aquifer, ChunkPos sourceChunkPos, CarvingMask mask) {
		// Carvers run once for every chunk in range; the cavern is one shape, so each chunk only carves itself
		if (!sourceChunkPos.equals(chunk.getPos()))
			return false;

		int minX = chunk.getPos().getMinBlockX();
		int minZ = chunk.getPos().getMinBlockZ();
		int quartMinY = QuartPos.fromBlock(FLOOR);
		int quartRows = QuartPos.fromBlock(CEILING) - quartMinY + 1;
		// Biome weight on the 4x4x4 biome grid, read straight from the chunk (cheap, unlike climate sampling)
		double[][][] weight = new double[4][quartRows][4];
		boolean any = false;
		for (int qx = 0; qx < 4; qx++)
			for (int qy = 0; qy < quartRows; qy++)
				for (int qz = 0; qz < 4; qz++) {
					boolean hollows = chunk.getNoiseBiome(QuartPos.fromBlock(minX) + qx, quartMinY + qy, QuartPos.fromBlock(minZ) + qz).is(BIOME);
					weight[qx][qy][qz] = hollows ? 1 : 0;
					any |= hollows;
				}
		if (!any)
			return false;

		Shape shape = new Shape(NOISES.computeIfAbsent(context.randomState(), CrystalHollowsCarver::createNoises), weight, chunk, minX, minZ);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean carved = false;
		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				for (int y = FLOOR; y <= shape.ceiling(lx, lz); y++) {
					if (!shape.open(lx, y, lz))
						continue;
					pos.set(minX + lx, y, minZ + lz);
					BlockState state = chunk.getBlockState(pos);
					// The biome's surface rule has already laid Crystalline Calcite on the floors of vanilla caves; it goes too,
					// or it would be left floating over the cavern
					if (state.isAir() || !(state.is(configuration.replaceable) || state.is(Blocks.LAVA) || state.is(Blocks.WATER)
							|| state.is(StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get())))
						continue;
					// Plain air, not cave air: MCreator's "is air" block predicate only matches minecraft:air
					chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), 0);
					mask.set(lx, y, lz);
					carved = true;
				}
			}
		}

		// Crust every surface of the cavern: Crystalline Calcite over calcite on the floors, calcite on walls, ceilings
		// and pillars. Top down, so a floor block is already crystalline when the block under it is reached.
		BlockState crystalline = StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get().defaultBlockState();
		BlockState calcite = Blocks.CALCITE.defaultBlockState();
		for (int lx = 0; lx < 16; lx++) {
			for (int lz = 0; lz < 16; lz++) {
				for (int y = shape.ceiling(lx, lz) + 1; y >= FLOOR - 2; y--) {
					if (biomeWeight(weight, lx, y - FLOOR, lz) <= 0.05)
						continue;
					pos.set(minX + lx, y, minZ + lz);
					BlockState state = chunk.getBlockState(pos);
					if (state.isAir() || !state.is(configuration.replaceable) || state.is(Blocks.WATER))
						continue;
					BlockState above = chunk.getBlockState(pos.above());
					BlockState crust = null;
					if (above.isAir())
						crust = crystalline;
					else if (above.is(crystalline.getBlock()) || shape.air(lx, y - 1, lz) || shape.air(lx - 1, y, lz) || shape.air(lx + 1, y, lz)
							|| shape.air(lx, y, lz - 1) || shape.air(lx, y, lz + 1))
						crust = calcite;
					if (crust != null) {
						chunk.setBlockState(pos, crust, 0);
						carved = true;
					}
				}
			}
		}
		return carved;
	}

	// The cavern's shape, known one block past the chunk edge so surfaces on the border can see the neighbour's
	// cavern before that chunk is carved
	private static class Shape {
		private final Noises noises;
		private final double[][][] weight;
		private final ChunkAccess chunk;
		private final int minX, minZ;
		// Cave noise every CELL blocks from one cell before the chunk to one cell past it
		private final double[][][] cave;
		private final int[][] ceilings = new int[18][18];
		private final double[][] pillars = new double[18][18];

		Shape(Noises noises, double[][][] weight, ChunkAccess chunk, int minX, int minZ) {
			this.noises = noises;
			this.weight = weight;
			this.chunk = chunk;
			this.minX = minX;
			this.minZ = minZ;
			int cellsXZ = 16 / CELL + 2;
			int cellsY = (CEILING - FLOOR) / CELL + 1;
			cave = new double[cellsXZ + 1][cellsY + 1][cellsXZ + 1];
			for (int cx = 0; cx <= cellsXZ; cx++)
				for (int cy = 0; cy <= cellsY; cy++)
					for (int cz = 0; cz <= cellsXZ; cz++)
						// y is stretched so chambers come out wide and flat rather than round
						cave[cx][cy][cz] = noises.cave.getValue(minX + (cx - 1) * CELL, (FLOOR + cy * CELL) * 1.8, minZ + (cz - 1) * CELL);
			for (int lx = -1; lx <= 16; lx++)
				for (int lz = -1; lz <= 16; lz++) {
					// Outside the chunk the ground isn't known, so the edge column's ceiling is used
					int hx = Mth.clamp(lx, 0, 15), hz = Mth.clamp(lz, 0, 15);
					ceilings[lx + 1][lz + 1] = hx == lx && hz == lz ? Math.min(CEILING, ground(chunk, minX + hx, minZ + hz) - ROOF) : Integer.MIN_VALUE;
					pillars[lx + 1][lz + 1] = noises.pillars.getValue(minX + lx, 0, minZ + lz);
				}
			for (int lx = -1; lx <= 16; lx++)
				for (int lz = -1; lz <= 16; lz++)
					if (ceilings[lx + 1][lz + 1] == Integer.MIN_VALUE)
						ceilings[lx + 1][lz + 1] = ceilings[Mth.clamp(lx, 0, 15) + 1][Mth.clamp(lz, 0, 15) + 1];
		}

		// Top solid block of the column (the sea floor under water). The worldgen heightmaps aren't filled in yet
		// when carvers run, so the column is scanned instead.
		private static int ground(ChunkAccess chunk, int x, int z) {
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, chunk.getMaxY(), z);
			for (; pos.getY() > FLOOR; pos.move(0, -1, 0)) {
				BlockState state = chunk.getBlockState(pos);
				if (!state.isAir() && state.getFluidState().isEmpty())
					return pos.getY();
			}
			return FLOOR;
		}

		int ceiling(int lx, int lz) {
			return ceilings[lx + 1][lz + 1];
		}

		boolean open(int lx, int y, int lz) {
			int ceiling = ceiling(lx, lz);
			if (y < FLOOR || y > ceiling || ceiling <= FLOOR + 8)
				return false;
			double w = biomeWeight(weight, lx, y - FLOOR, lz);
			if (w <= 0.05)
				return false;
			double t = (y - FLOOR) / (double) (ceiling - FLOOR);
			double shell = Math.max(0, SHELL - t) / SHELL + Math.max(0, t - (1 - SHELL)) / SHELL;
			double density = -0.35 - 0.9 * caveNoise(lx + CELL, y - FLOOR, lz + CELL) + 1.3 * shell * shell + 2.5 * (1 - w);
			if (density >= 0)
				return false;
			// Pillars flare out toward the floor and ceiling like they grew into the rock
			double edge = 2 * t - 1;
			return pillars[lx + 1][lz + 1] + 0.06 * noises.wobble.getValue(minX + lx, y, minZ + lz) <= 0.5 - 0.22 * edge * edge;
		}

		// Air in this chunk, or cavern the neighbouring chunk will carve
		boolean air(int lx, int y, int lz) {
			if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16)
				return chunk.getBlockState(new BlockPos(minX + lx, y, minZ + lz)).isAir();
			return open(lx, y, lz);
		}

		private double caveNoise(int gx, int dy, int gz) {
			int cx = gx / CELL, cy = dy / CELL, cz = gz / CELL;
			double fx = (gx % CELL) / (double) CELL, fy = (dy % CELL) / (double) CELL, fz = (gz % CELL) / (double) CELL;
			return Mth.lerp3(fx, fy, fz, cave[cx][cy][cz], cave[cx + 1][cy][cz], cave[cx][cy + 1][cz], cave[cx + 1][cy + 1][cz],
				cave[cx][cy][cz + 1], cave[cx + 1][cy][cz + 1], cave[cx][cy + 1][cz + 1], cave[cx + 1][cy + 1][cz + 1]);
		}
	}

	private static Noises createNoises(RandomState randomState) {
		PositionalRandomFactory random = randomState.getOrCreateRandomFactory(Identifier.fromNamespaceAndPath("strawberrymc", "crystal_hollows"));
		return new Noises(NormalNoise.create(random.fromHashOf("cave"), -7, 1.0, 1.0, 0.5, 0.25),
			NormalNoise.create(random.fromHashOf("pillars"), -5, 1.0, 0.6, 0.3),
			NormalNoise.create(random.fromHashOf("wobble"), -3, 1.0, 0.5));
	}

	// Smooth blend between biome cells so the cavern edge fades into rock instead of stepping in 4-block cubes
	private static double biomeWeight(double[][][] weight, int lx, int dy, int lz) {
		double fx = Mth.clamp((lx - 1.5) / 4, 0, 3), fy = Mth.clamp((dy - 1.5) / 4, 0, weight[0].length - 1), fz = Mth.clamp((lz - 1.5) / 4, 0, 3);
		int x0 = Math.min((int) fx, 2), y0 = Math.min((int) fy, weight[0].length - 2), z0 = Math.min((int) fz, 2);
		return Mth.lerp3(fx - x0, fy - y0, fz - z0, weight[x0][y0][z0], weight[x0 + 1][y0][z0], weight[x0][y0 + 1][z0], weight[x0 + 1][y0 + 1][z0],
			weight[x0][y0][z0 + 1], weight[x0 + 1][y0][z0 + 1], weight[x0][y0 + 1][z0 + 1], weight[x0 + 1][y0 + 1][z0 + 1]);
	}

	public static void init(IEventBus eventBus) {
		CARVERS.register(eventBus);
	}
}

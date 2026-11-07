package io.github.strawberrymc.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import io.github.strawberrymc.init.StrawberrymcModBlocks;

public class CalciteMountainGenerationProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double radius = 0;
		double height = 0;
		double dx = 0;
		double dz = 0;
		double dist = 0;
		double top = 0;
		double k = 0;
		radius = Mth.nextInt(RandomSource.create(), 7, 13);
		height = Mth.nextInt(RandomSource.create(), (int) (radius * 0.8), (int) (radius * 1.6));
		dx = (0 - radius);
		while (dx <= radius) {
			dz = (0 - radius);
			while (dz <= radius) {
				dist = (Math.sqrt(((dx * dx) + (dz * dz))) / radius);
				if (dist <= 1) {
					top = Math.round(((height * Math.pow((1 - dist), 1.4)) + Mth.nextDouble(RandomSource.create(), -0.6, 1.2)));
					if (top >= 0) {
						k = top;
						while ((k > -12) && (world.isEmptyBlock(BlockPos.containing((x + dx), (y + k), (z + dz))))) {
							world.setBlock(BlockPos.containing((x + dx), (y + k), (z + dz)), Blocks.CALCITE.defaultBlockState(), 3);
							k = (k - 1);
						}
						if (world.isEmptyBlock(BlockPos.containing((x + dx), ((y + top) + 1), (z + dz)))) {
							world.setBlock(BlockPos.containing((x + dx), ((y + top) + 1), (z + dz)), StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get().defaultBlockState(), 3);
						}
					}
				}
				dz = (dz + 1);
			}
			dx = (dx + 1);
		}
	}
}
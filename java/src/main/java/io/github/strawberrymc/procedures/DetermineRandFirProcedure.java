package io.github.strawberrymc.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class DetermineRandFirProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double randint = 0;
		randint = Mth.nextInt(RandomSource.create(), 1, 100);
		if (randint < 85) {
			FirGenerationProcedure.execute(world, x, y, z);
		} else {
			randint = Mth.nextInt(RandomSource.create(), 1, 100);
			if (randint < 60) {
				DeadFirGenerationProcedure.execute(world, x, y, z);
			} else {
				FirSnagGenerationProcedure.execute(world, x, y, z);
			}
		}
	}
}

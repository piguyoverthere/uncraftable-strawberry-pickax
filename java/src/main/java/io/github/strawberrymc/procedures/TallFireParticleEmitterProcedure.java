package io.github.strawberrymc.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class TallFireParticleEmitterProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		for (int _i1 = 0; _i1 < Mth.nextInt(RandomSource.create(), 2, 5); _i1++) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.LAVA, (x + 0.5), (y + 0.5), (z + 0.5), 1, 0.5, 0.5, 0.5, 2);
		}
	}
}
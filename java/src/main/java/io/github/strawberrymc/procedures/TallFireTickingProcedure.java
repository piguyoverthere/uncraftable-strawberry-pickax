package io.github.strawberrymc.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;

public class TallFireTickingProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double randx = 0;
		double randy = 0;
		double randz = 0;
		if (world instanceof Level _lvl0 && _lvl0.isRaining() && world.canSeeSkyFromBelowWater(BlockPos.containing(x, y, z))) {
			world.setBlock(BlockPos.containing(x, y, z), Blocks.AIR.defaultBlockState(), 3);
		} else {
			for (int _i1 = 0; _i1 < 15; _i1++) {
				randx = Mth.nextInt(RandomSource.create(), -6, 6);
				randy = Mth.nextInt(RandomSource.create(), -6, 6);
				randz = Mth.nextInt(RandomSource.create(), -6, 6);
			}
			if ((world.getBlockState(BlockPos.containing(randx, randy, randz))).is(BlockTags.create(Identifier.parse("mod:dry_ignition")))) {
				world.setBlock(BlockPos.containing(randx, randy, randz), Blocks.FIRE.defaultBlockState(), 3);
			}
			world.setBlock(BlockPos.containing(randx, randy + 1, randz), Blocks.FIRE.defaultBlockState(), 3);
		}
	}
}
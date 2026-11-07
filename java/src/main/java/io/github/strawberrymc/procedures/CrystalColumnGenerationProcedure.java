package io.github.strawberrymc.procedures;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import io.github.strawberrymc.init.StrawberrymcModBlocks;

public class CrystalColumnGenerationProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		BlockState mat = Blocks.AIR.defaultBlockState();
		double pick = 0;
		double h = 0;
		double lean_x = 0;
		double lean_z = 0;
		double radius = 0;
		double rot = 0;
		double n1x = 0;
		double n1z = 0;
		double n2x = 0;
		double n2z = 0;
		double n3x = 0;
		double n3z = 0;
		double i = 0;
		double r = 0;
		double reach = 0;
		double cx = 0;
		double cz = 0;
		double dx = 0;
		double dz = 0;
		pick = Mth.nextInt(RandomSource.create(), 1, 6);
		if (pick == 1) {
			mat = Blocks.AMETHYST_BLOCK.defaultBlockState();
		} else if (pick == 2) {
			mat = StrawberrymcModBlocks.GARNET_BLOCK.get().defaultBlockState();
		} else if (pick == 3) {
			mat = StrawberrymcModBlocks.DENDRITE_BLOCK.get().defaultBlockState();
		} else if (pick == 4) {
			mat = StrawberrymcModBlocks.CITRINE_BLOCK.get().defaultBlockState();
		} else if (pick == 5) {
			mat = StrawberrymcModBlocks.AQUAMARINE_BLOCK.get().defaultBlockState();
		} else {
			mat = StrawberrymcModBlocks.ROSE_QUARTZ_BLOCK.get().defaultBlockState();
		}
		h = 0;
		while ((world.isEmptyBlock(BlockPos.containing(x, (y + h), z))) && (h < 120)) {
			h = (h + 1);
		}
		if ((h >= 8) && (h < 120)) {
			lean_x = (Mth.nextDouble(RandomSource.create(), -5, 5) / (h + 3));
			lean_z = (Mth.nextDouble(RandomSource.create(), -5, 5) / (h + 3));
			radius = Mth.nextInt(RandomSource.create(), 3, 5);
			rot = Mth.nextDouble(RandomSource.create(), 0, 2.0944);
			n1x = Math.cos(rot);
			n1z = Math.sin(rot);
			n2x = Math.cos((rot + 2.0944));
			n2z = Math.sin((rot + 2.0944));
			n3x = Math.cos((rot + 4.1888));
			n3z = Math.sin((rot + 4.1888));
			i = -3;
			while (i <= (h + 3)) {
				r = (radius * (1 + (0.5 * Math.pow(Math.min(Math.abs((((2 * i) / h) - 1)), 1), 4))));
				reach = (Math.round(r) + 1);
				cx = (x + (lean_x * i));
				cz = (z + (lean_z * i));
				dx = (0 - reach);
				while (dx <= reach) {
					dz = (0 - reach);
					while (dz <= reach) {
						if (((((dx * n1x) + (dz * n1z)) <= (r / 2)) && (((dx * n2x) + (dz * n2z)) <= (r / 2))) && (((dx * n3x) + (dz * n3z)) <= (r / 2))) {
							if ((world.isEmptyBlock(BlockPos.containing((cx + dx), (y + i), (cz + dz)))) || ((world.getBlockState(BlockPos.containing((cx + dx), (y + i), (cz + dz)))).is(BlockTags.create(Identifier.parse("minecraft:base_stone_overworld"))))) {
								world.setBlock(BlockPos.containing((cx + dx), (y + i), (cz + dz)), mat, 3);
							}
						}
						dz = (dz + 1);
					}
					dx = (dx + 1);
				}
				i = (i + 1);
			}
		}
	}
}
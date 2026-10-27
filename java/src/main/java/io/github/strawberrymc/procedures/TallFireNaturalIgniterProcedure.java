package io.github.strawberrymc.procedures;

import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

@EventBusSubscriber
public class TallFireNaturalIgniterProcedure {
	@SubscribeEvent
	public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		execute(null, world, x, y, z);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z) {
		double fire_tracking = 0;
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == Blocks.FIRE) {
			if ((world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if ((world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if ((world.getBlockState(BlockPos.containing(x, y + 1, z))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if ((world.getBlockState(BlockPos.containing(x, y - 1, z))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if ((world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if ((world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == Blocks.FIRE) {
				fire_tracking = fire_tracking + 1;
			}
			if (fire_tracking >= 2 && Math.random() < 0.5) {
				world.setBlock(BlockPos.containing(x, y, z), StrawberrymcModBlocks.TALL_FIRE.get().defaultBlockState(), 3);
			}
		}
	}
}
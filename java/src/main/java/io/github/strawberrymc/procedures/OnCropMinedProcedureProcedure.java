package io.github.strawberrymc.procedures;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

@EventBusSubscriber
public class OnCropMinedProcedureProcedure {
	@SubscribeEvent
	public void onBlockBreak(BreakBlockEvent event) {
		if (event.getState().is(Blocks.CARROTS) || event.getState().is(Blocks.POTATOES) || event.getState().is(Blocks.WHEAT) || event.getState().is(Blocks.BEETROOTS) || event.getState().is(Blocks.TORCHFLOWER_CROP) || event.getState().is(Blocks.PITCHER_CROP)) {
			BlockPos farmlandPos = new BlockPos(event.getPos().getX(), event.getPos().getY()-1, event.getPos().getZ());
			Level world = (Level) event.getLevel();
			BlockState farmlandState = world.getBlockState(farmlandPos);
			if (farmlandState.is(Blocks.FARMLAND)) {
				world.setBlock(farmlandPos, StrawberrymcModBlocks.USED_FARMLAND.get().defaultBlockState(), 3);
			}
			if (farmlandState.is(StrawberrymcModBlocks.USED_FARMLAND)) {
				world.setBlock(farmlandPos, StrawberrymcModBlocks.USED_FARMLAND.get().defaultBlockState(), 3);
			}
		}
	}
}
package io.github.strawberrymc.procedures;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import io.github.strawberrymc.init.StrawberrymcModBlocks;
import io.github.strawberrymc.block.UsedFarmlandBlock;

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
				int currentState = farmlandState.getValue(UsedFarmlandBlock.USES);
				int nextStage = currentState < 5 ? currentState + 1 : 0;

				world.setBlock(farmlandPos, state.setValue(UsedFarmlandBlock.STAGE, nextStage), 3);
				if (currentState == 5) {
					world.setBlock(farmlandPos, StrawberrymcModBlocks.POOR_FARMLAND.get().defaultBlockState(), 3);
				}
			}
		}
	}
}

package io.github.strawberrymc.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

public class FarmlandDegradeProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world == null) return;
        
        BlockPos brokenBlockPos = BlockPos.containing(x, y, z);
        BlockState brokenBlockState = world.getBlockState(brokenBlockPos);

        if (brokenBlockState.is(BlockTags.CROPS)) {
            BlockPos farmlandPos = BlockPos.containing(x, y - 1, z);
            BlockState farmlandState = world.getBlockState(farmlandPos);

            // 1. Check if it is the Farmland Clone (degrades to Poor Farmland)
            if (farmlandState.getBlock() == StrawberrymcModBlocks.USED_FARMLAND.get()) {
                
            } 
            // 2. Check if it is Fertile Farmland (degrades to Farmland Clone)
            else if (farmlandState.getBlock() == StrawberrymcModBlocks.FERTILE_FARMLAND.get()) {
                BlockEntity blockEntity = world.getBlockEntity(farmlandPos);
                if (blockEntity != null) {
                    double currentHarvests = (double) blockEntity.getPersistentData().getDouble("HarvestCount").orElse(0.0);
                    if (currentHarvests >= 3) {
                        world.setBlock(farmlandPos, Blocks.FARMLAND.defaultBlockState(), 3);
                    } else {
                        blockEntity.getPersistentData().putDouble("HarvestCount", currentHarvests + 1);
                    }
                }
            }
        }
    }
}
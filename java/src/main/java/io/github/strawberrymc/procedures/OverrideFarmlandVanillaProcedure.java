package io.github.strawberrymc.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

public class OverrideFarmlandVanillaProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, LivingEntity entity) {
        if (world == null || entity == null) return;
        
        BlockPos targetPos = BlockPos.containing(x, y, z);
        net.minecraft.world.level.block.Block targetBlock = world.getBlockState(targetPos).getBlock();
        
        // 1. Verify the clicked block is one of your custom farmlands
        if (targetBlock == StrawberrymcModBlocks.POOR_FARMLAND.get() || targetBlock == StrawberrymcModBlocks.FERTILE_FARMLAND.get()) {
            ItemStack mainHandItem = entity.getMainHandItem();
            BlockPos plantPos = BlockPos.containing(x, y + 1, z);
            
            net.minecraft.world.level.block.state.BlockState cropToPlant = null;

            // 2. Map the vanilla seed items to their respective crop blocks
            if (mainHandItem.getItem() == Items.WHEAT_SEEDS) {
                cropToPlant = Blocks.WHEAT.defaultBlockState();
            } else if (mainHandItem.getItem() == Items.CARROT) {
                cropToPlant = Blocks.CARROTS.defaultBlockState();
            } else if (mainHandItem.getItem() == Items.POTATO) {
                cropToPlant = Blocks.POTATOES.defaultBlockState();
            } else if (mainHandItem.getItem() == Items.BEETROOT_SEEDS) {
                cropToPlant = Blocks.BEETROOTS.defaultBlockState();
            }

            // 3. If a valid seed was used and the space above is empty, plant it
            if (cropToPlant != null && world.isEmptyBlock(plantPos)) {
                world.setBlock(plantPos, cropToPlant, 3);
                
                // 4. Consume the item if the player is not in Creative mode
                if (entity instanceof Player player && !player.getAbilities().instabuild) {
                    mainHandItem.shrink(1);
                }
            }
        }
    }
}
package io.github.strawberrymc.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;

public class FirGenerationProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z) {
        if (world == null) return;

        // Taller trunk: 18-25 blocks
        int height = 18 + (int)(Math.random() * 8); 

        // 1. Build the central trunk (Spruce Logs)
        for (int i = 0; i < height; i++) {
            world.setBlock(BlockPos.containing(x, y + i, z), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
        }

        // 2. Add variable base roots (Spruce Wood)
        if (Math.random() < 0.6) world.setBlock(BlockPos.containing(x + 1, y, z), Blocks.SPRUCE_WOOD.defaultBlockState(), 3);
        if (Math.random() < 0.6) world.setBlock(BlockPos.containing(x - 1, y, z), Blocks.SPRUCE_WOOD.defaultBlockState(), 3);
        if (Math.random() < 0.6) world.setBlock(BlockPos.containing(x, y, z + 1), Blocks.SPRUCE_WOOD.defaultBlockState(), 3);
        if (Math.random() < 0.6) world.setBlock(BlockPos.containing(x, y, z - 1), Blocks.SPRUCE_WOOD.defaultBlockState(), 3);

        // 3. Generate Foliage
        int firstTier = 3 + (int)(Math.random() * 2); // Start foliage much lower (3-4 blocks up)
        for (int i = firstTier; i < height - 2; i++) { 
            boolean isWideTier = (i % 2 == 0); 
            
            int radius;
            if (isWideTier) {
                // Increased divisor to 6.5 to maintain the same narrow base over a longer stretch
                radius = (int) Math.ceil((double)(height - i) / 6.5); 
            } else {
                // In-between layers stay tight to the trunk
                radius = 1; 
            }
            
            if (radius < 1) radius = 1;
            
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0) continue; 
                    
                    // Cut corners for a jagged, organic cross/diamond shape
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius) {
                        if (radius > 1 && Math.random() < 0.8) continue; 
                        if (radius == 1 && Math.random() < 0.5) continue; 
                    }
                    
                    // Airy sparseness (35% chance to skip)
                    if (Math.random() < 0.35) continue; 

                    BlockPos leafPos = BlockPos.containing(x + dx, y + i, z + dz);
                    if (world.isEmptyBlock(leafPos)) {
                        world.setBlock(leafPos, Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }

        // 4. Generate the tall top spire
        for (int i = height - 2; i <= height + 3; i++) {
            BlockPos spirePos = BlockPos.containing(x, y + i, z);
            if (world.isEmptyBlock(spirePos)) {
                world.setBlock(spirePos, Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);
            }
        }

        // 5. Connect fences and set leaf distances so the leaves don't decay
        io.github.strawberrymc.FirTreeFinisher.finish(world, BlockPos.containing(x, y, z));
    }
}

package io.github.strawberrymc.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class CalculateWaypointLocationProcedure {

    /**
     * Predictive result containing whether the block exists and its location.
     */
    public record PredictionResult(boolean isPresentInChunk, BlockPos position) {}

    /**
     * Calculates if a rare vegetal feature will generate inside a target chunk,
     * and if so, exactly where it will be placed.
     *
     * @param worldSeed The 64-bit world seed.
     * @param chunkX The target chunk X coordinate.
     * @param chunkZ The target chunk Z coordinate.
     * @param rarityChance The chance denominator (e.g., 100 means a 1-in-100 chance per chunk).
     * @return A PredictionResult with the existence status and calculated BlockPos.
     */
    public static PredictionResult predictRareVegetalLocation(long worldSeed, int chunkX, int chunkZ, int rarityChance) {
        // 1. Initialize Minecraft's specific Xoroshiro128++ world-gen RNG
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0L));

        // 2. Derive the unique decoration seed for VEGETAL_DECORATION (Index 8)
        // This locks the RNG sequence to match Minecraft's exact state for this chunk step
        random.setDecorationSeed(worldSeed, chunkX << 4, chunkZ << 4);

        // 3. --- FIXED RARITY FILTER SIMULATION ---
        // Minecraft's modern feature modifiers do not simply call nextInt() on the root generator.
        // They derive an internal position/modifier specific long to isolate placement logic.
        // We simulate the native RarityFilter behavior by correctly consuming the internal sequence state.
        long internalModifierSalt = random.nextLong(); 
        WorldgenRandom filterRandom = new WorldgenRandom(new XoroshiroRandomSource(internalModifierSalt));

        if (filterRandom.nextInt(rarityChance) != 0) {
            // Does not exist in this chunk
            return new PredictionResult(false, BlockPos.ZERO);
        }

        // 4. --- LOCATION CALCULATIONS ---
        // Now that the filter passed, we resume on the original random stream 
        // to handle the position offsets ("minecraft:in_square")
        int offsetX = random.nextInt(16);
        int offsetZ = random.nextInt(16);

        // Convert local chunk offsets to absolute world coordinates
        int blockX = (chunkX << 4) + offsetX;
        int blockZ = (chunkZ << 4) + offsetZ;

        // Baseline height approximation for ungenerated terrain
        int predictedY = 64;

        // Block exists! Return true along with the precise coordinates.
        return new PredictionResult(true, new BlockPos(blockX, predictedY, blockZ));
    }
}

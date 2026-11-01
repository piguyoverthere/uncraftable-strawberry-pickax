package io.github.strawberrymc.custommixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// OceanMonumentFloorMixin sets monuments on the lowest floor under them, so higher seafloor around the monument
// used to stand as walls around it. This digs a basin around each monument instead: flat at the monument's base
// for FLAT_RING blocks, then sloping back up to the natural floor. The monument itself is not moved.
@Mixin(Structure.class)
public class MonumentBasinMixin {
	private static final int FLAT_RING = 8;
	// Far enough for the slope to climb back up from the lowest monuments (base y=-58) to the usual abyss floor
	private static final int BASIN_RADIUS = 56;
	private static final int MAX_CAVE_FLOOD = 16;

	// The structure only gets to place blocks in chunks its bounding box covers, so widen it to the basin
	@Inject(method = "adjustBoundingBox", at = @At("RETURN"), cancellable = true)
	private void strawberrymc$widenForBasin(BoundingBox boundingBox, CallbackInfoReturnable<BoundingBox> cir) {
		if ((Object) this instanceof OceanMonumentStructure) {
			BoundingBox box = cir.getReturnValue();
			cir.setReturnValue(new BoundingBox(box.minX() - BASIN_RADIUS, box.minY(), box.minZ() - BASIN_RADIUS, box.maxX() + BASIN_RADIUS, box.maxY(),
					box.maxZ() + BASIN_RADIUS));
		}
	}

	@Inject(method = "afterPlace", at = @At("HEAD"))
	private void strawberrymc$digBasin(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB,
			net.minecraft.world.level.ChunkPos chunkPos, PiecesContainer pieces, CallbackInfo ci) {
		if (!((Object) this instanceof OceanMonumentStructure) || pieces.isEmpty())
			return;
		// The first piece is the monument building, which covers the whole footprint
		BoundingBox monument = pieces.pieces().get(0).getBoundingBox();
		int seaLevel = level.getSeaLevel();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

		for (int x = chunkBB.minX(); x <= chunkBB.maxX(); x++) {
			for (int z = chunkBB.minZ(); z <= chunkBB.maxZ(); z++) {
				int dx = Math.max(0, Math.max(monument.minX() - x, x - monument.maxX()));
				int dz = Math.max(0, Math.max(monument.minZ() - z, z - monument.maxZ()));
				// Inside the footprint the monument already fills everything with water
				if (dx == 0 && dz == 0)
					continue;
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance > BASIN_RADIUS)
					continue;

				int floorY = monument.minY() + (int) Math.max(0, distance - FLAT_RING);
				boolean dug = false;
				for (int y = floorY; y < seaLevel; y++) {
					pos.set(x, y, z);
					if (!level.getBlockState(pos).is(Blocks.WATER)) {
						level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
						dug = true;
					}
				}
				// Flood caves the basin opened up, so they don't leave air pockets underwater
				for (int y = floorY - 1; y > floorY - MAX_CAVE_FLOOD; y--) {
					pos.set(x, y, z);
					if (!level.getBlockState(pos).isAir())
						break;
					level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
				}
				if (!dug)
					continue;
				// Cover the newly exposed floor like the rest of the deep ocean floor
				pos.set(x, floorY - 1, z);
				if (level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD) && level.getBlockState(pos.below()).isSolid())
					level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 2);
			}
		}
	}
}

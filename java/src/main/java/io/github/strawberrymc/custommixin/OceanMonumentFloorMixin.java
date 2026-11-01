package io.github.strawberrymc.custommixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla always builds ocean monuments with their base at y=39. The Ocean Abyss sea floor is much deeper
// (see data/minecraft/worldgen/density_function/overworld/offset.json), so this lowers each monument onto the
// lowest floor under its footprint. The monument fills its own footprint with water up to sea level, so any
// higher floor inside it is cleared rather than buried.
@Mixin(OceanMonumentStructure.class)
public class OceanMonumentFloorMixin {
	// Bedrock reaches up to y=-60, so this keeps a little deepslate under the monument (AbyssLavaFlood keeps the
	// water around it from turning to lava below y=-54)
	private static final int LOWEST_BASE_Y = -58;

	@Inject(method = "generatePieces", at = @At("TAIL"))
	private static void strawberrymc$sitOnSeafloor(StructurePiecesBuilder builder, Structure.GenerationContext context, CallbackInfo ci) {
		BoundingBox box = builder.getBoundingBox();
		int lowestFloor = Integer.MAX_VALUE;
		// Sample a 5x5 grid across the 58x58 footprint
		for (int i = 0; i <= 4; i++) {
			for (int j = 0; j <= 4; j++) {
				int x = box.minX() + (box.maxX() - box.minX()) * i / 4;
				int z = box.minZ() + (box.maxZ() - box.minZ()) * j / 4;
				// First non-solid block above the floor, so the floor block itself is one below
				int floor = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()) - 1;
				lowestFloor = Math.min(lowestFloor, floor);
			}
		}
		int dy = Math.max(lowestFloor, LOWEST_BASE_Y) - box.minY();
		// Only ever move down; vanilla height is fine wherever the floor is higher
		if (dy < 0)
			builder.offsetPiecesVertically(dy);
	}

	// Monument pieces are rebuilt at y=39 whenever the chunk is loaded from disk, so carry over the saved height
	@Inject(method = "regeneratePiecesAfterLoad", at = @At("RETURN"))
	private static void strawberrymc$keepSavedHeight(ChunkPos chunkPos, long seed, PiecesContainer savedPieces, CallbackInfoReturnable<PiecesContainer> cir) {
		PiecesContainer rebuilt = cir.getReturnValue();
		if (savedPieces.isEmpty() || rebuilt.isEmpty())
			return;
		int dy = savedPieces.pieces().get(0).getBoundingBox().minY() - rebuilt.pieces().get(0).getBoundingBox().minY();
		if (dy != 0) {
			for (StructurePiece piece : rebuilt.pieces())
				piece.move(0, dy, 0);
		}
	}
}

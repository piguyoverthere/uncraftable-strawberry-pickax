package io.github.strawberrymc.custommixin;

import io.github.strawberrymc.monument.MonumentShell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Ocean monuments are built twice as big and hollow by MonumentShell: the building's box is doubled around the same
// centre, and instead of vanilla's walls and rooms it places vanilla's outer shell at double scale, the roof gazebo
// and the Prismatic Sensors. MonumentShell itself runs vanilla's own code (without the rooms) to record the shell.
@Mixin(OceanMonumentPieces.MonumentBuilding.class)
public class MonumentBuildingShellMixin {
	@Inject(method = "<init>(Lnet/minecraft/util/RandomSource;IILnet/minecraft/core/Direction;)V", at = @At("TAIL"))
	private void strawberrymc$doubleSize(RandomSource random, int west, int north, Direction direction, CallbackInfo ci) {
		if (MonumentShell.isRecording())
			return;
		StructurePiece self = (StructurePiece) (Object) this;
		((StructurePieceBoxAccessor) self).strawberrymc$setBoundingBox(MonumentShell.scaledBox(self.getBoundingBox()));
	}

	@Inject(method = "postProcess", at = @At("HEAD"), cancellable = true)
	private void strawberrymc$buildShell(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos, CallbackInfo ci) {
		if (MonumentShell.isRecording())
			return;
		StructurePiece self = (StructurePiece) (Object) this;
		MonumentShell.place(level, chunkBB, self.getBoundingBox(), self.getOrientation());
		ci.cancel();
	}
}

package io.github.strawberrymc.custommixin;

import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Vanilla never moves monuments, so moving the building doesn't move its rooms. Used by OceanMonumentFloorMixin.
@Mixin(StructurePiece.class)
public class StructurePieceMoveMixin {
	@Inject(method = "move", at = @At("TAIL"))
	private void strawberrymc$moveMonumentRooms(int dx, int dy, int dz, CallbackInfo ci) {
		if ((Object) this instanceof OceanMonumentPieces.MonumentBuilding building) {
			for (StructurePiece room : ((MonumentBuildingAccessor) building).strawberrymc$getChildPieces())
				room.move(dx, dy, dz);
		}
	}
}

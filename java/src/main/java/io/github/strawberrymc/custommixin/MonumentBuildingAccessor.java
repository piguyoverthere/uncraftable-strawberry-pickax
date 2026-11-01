package io.github.strawberrymc.custommixin;

import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// The monument's rooms live in a private list separate from the building piece itself
@Mixin(OceanMonumentPieces.MonumentBuilding.class)
public interface MonumentBuildingAccessor {
	@Accessor("childPieces")
	List<? extends StructurePiece> strawberrymc$getChildPieces();
}

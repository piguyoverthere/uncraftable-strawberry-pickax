package io.github.strawberrymc.custommixin;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Lets MonumentBuildingShellMixin resize the monument building after vanilla has built it
@Mixin(StructurePiece.class)
public interface StructurePieceBoxAccessor {
	@Accessor("boundingBox")
	void strawberrymc$setBoundingBox(BoundingBox box);
}

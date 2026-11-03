package io.github.strawberrymc.custommixin;

import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// Lets the monument arena fix its rune frame in place, like the Fixed tag
@Mixin(ItemFrame.class)
public interface ItemFrameAccessor {
	@Accessor("fixed")
	void strawberrymc$setFixed(boolean fixed);
}

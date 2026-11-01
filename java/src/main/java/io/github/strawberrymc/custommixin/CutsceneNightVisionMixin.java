package io.github.strawberrymc.custommixin;

import io.github.strawberrymc.cutscene.MonumentCutscene;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Vanilla night vision is either full strength or flickering as it runs out. The monument cutscene's night vision
// fades smoothly in and out instead.
@Mixin(GameRenderer.class)
public class CutsceneNightVisionMixin {
	@Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
	private static void strawberrymc$cutsceneFade(LivingEntity camera, float partialTicks, CallbackInfoReturnable<Float> cir) {
		float scale = MonumentCutscene.nightVisionScale(partialTicks);
		if (scale >= 0)
			cir.setReturnValue(scale);
	}
}

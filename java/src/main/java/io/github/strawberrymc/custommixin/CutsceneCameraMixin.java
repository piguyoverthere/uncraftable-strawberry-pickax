package io.github.strawberrymc.custommixin;

import io.github.strawberrymc.cutscene.MonumentCutscene;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// While the monument cutscene plays, the camera is placed at its shot instead of the player's eyes. The player stays
// the camera entity, so nothing else about the game changes.
@Mixin(Camera.class)
public abstract class CutsceneCameraMixin {
	@Shadow
	protected abstract void setPosition(Vec3 position);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "alignWithEntity", at = @At("TAIL"))
	private void strawberrymc$cutsceneShot(float partialTicks, CallbackInfo ci) {
		MonumentCutscene.Shot shot = MonumentCutscene.currentShot(partialTicks);
		if (shot != null) {
			this.setRotation(shot.yaw(), shot.pitch());
			this.setPosition(shot.position());
		}
	}
}

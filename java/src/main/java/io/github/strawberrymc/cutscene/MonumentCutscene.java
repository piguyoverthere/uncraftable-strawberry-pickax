package io.github.strawberrymc.cutscene;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import javax.annotation.Nullable;

// Cutscene played the first time a player enters an ocean monument (started by MonumentIntro): the spooky chime plays,
// the camera pans out to a diagonal view of the monument, sweeps sideways around it with "Ocean Monument" in the
// corner, then pans back to the player. While it runs the HUD, hand, water overlay and water fog are hidden and night
// vision lights up the monument, which sits too deep for any daylight to reach.
//
// The camera never passes through blocks: during the sweep it keeps a clear line of sight from just above the monument's
// roof, pulling in when terrain is in the way and easing back out once it clears. When the path between the player and
// the sweep is blocked (as it is from inside the monument), it dips to black and cuts instead of flying through walls.
// CutsceneCameraMixin applies the shot to the camera every frame.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class MonumentCutscene {
	private static final SoundEvent CHIME = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("strawberrymc", "monument_chime"));

	// Matches the 8.1 second chime
	private static final int DURATION = 162;
	private static final int PAN_IN = 30;
	private static final int PAN_OUT = 26;
	// Half of a dip to black, for when the pan path is blocked
	private static final int DIP = 10;
	// Total sideways sweep around the monument, centred on the diagonal nearest the player
	private static final float SWEEP_DEGREES = 50;
	// How far the camera stays from whatever is blocking its view
	private static final double CLIP_MARGIN = 1.0;
	private static final double MIN_DISTANCE = 4.0;
	// Fraction of the remaining distance regained per tick once an obstruction clears
	private static final double CLIP_RECOVERY = 0.12;
	// Night vision and the cleared fog fade in at the start, then fade out across the pan back and this many ticks
	// after the cutscene
	private static final int FADE_IN = 20;
	private static final int FADE_TAIL = 40;
	private static final int TITLE_COLOR = 0xD8F4FF;

	public record Shot(Vec3 position, float yaw, float pitch) {
	}

	private static boolean active;
	private static int ticks;
	private static Vec3 center;
	private static Vec3 lookTarget;
	// Clear point just above the roof that the sweep's line of sight is checked from
	private static Vec3 anchor;
	private static double radius;
	private static double height;
	private static float startAngle;
	private static boolean dipIn;
	private static @Nullable Boolean dipOut;
	private static double clipDistance;
	private static float lastFrameTime;
	private static boolean addedNightVision;
	// Night vision and fog keep fading after the cutscene ends
	private static boolean fading;
	private static int fadeTicks;

	public static void start(BlockPos min, BlockPos max) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || active)
			return;
		center = new Vec3((min.getX() + max.getX() + 1) / 2.0, min.getY(), (min.getZ() + max.getZ() + 1) / 2.0);
		lookTarget = center.add(0, (max.getY() - min.getY()) * 0.4, 0);
		anchor = new Vec3(center.x, max.getY() + 2.5, center.z);
		radius = Math.max(max.getX() - min.getX(), max.getZ() - min.getZ()) * 1.05;
		height = max.getY() + 12 - center.y;
		// Diagonal nearest to the side the player came from
		double playerAngle = Math.toDegrees(Math.atan2(player.getZ() - center.z, player.getX() - center.x));
		startAngle = (float) (Math.round((playerAngle - 45) / 90) * 90 + 45 - SWEEP_DEGREES / 2);

		active = true;
		ticks = 0;
		lastFrameTime = 0;
		dipOut = null;
		clipDistance = Double.MAX_VALUE;
		Vec3 eye = player.getEyePosition();
		Vec3 firstShot = clipFromAnchor(orbitPosition(0), Double.MAX_VALUE);
		dipIn = isBlocked(eye, firstShot);

		// Its strength comes from nightVisionScale, so the duration only has to outlast the fade. A night vision effect the
		// player already had is left alone (unless it's this cutscene's own, still fading out)
		if (!addedNightVision)
			addedNightVision = !player.hasEffect(MobEffects.NIGHT_VISION);
		fading = true;
		fadeTicks = 0;
		if (addedNightVision)
			player.getActiveEffectsMap().put(MobEffects.NIGHT_VISION, new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION + 400, 0, true, false, false));
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(CHIME, 1.0F, 1.0F));
	}

	public static boolean isActive() {
		return active;
	}

	// Strength of the cutscene's night vision (0 is normal lighting), or -1 when the vanilla value should be used
	public static float nightVisionScale(float partialTicks) {
		return addedNightVision && fading ? fadeStrength(partialTicks) : -1;
	}

	// 1 while the cutscene's lighting and clear water are fully on, 0 when they're off
	private static float fadeStrength(float partialTicks) {
		float time = fadeTicks + partialTicks;
		return Math.min(ease(time / FADE_IN), 1 - ease((time - (DURATION - PAN_OUT)) / (PAN_OUT + FADE_TAIL)));
	}

	// The camera shot for this frame, or null when no cutscene is playing
	public static @Nullable Shot currentShot(float partialTicks) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!active || player == null)
			return null;
		float time = Math.min(ticks + partialTicks, DURATION);
		Vec3 eye = player.getEyePosition(partialTicks);
		float eyeYaw = player.getViewYRot(partialTicks);
		float eyePitch = player.getViewXRot(partialTicks);

		// The sweep's line of sight shrinks instantly when blocked and grows back smoothly
		Vec3 desired = orbitPosition(time);
		double free = freeDistance(anchor, desired);
		double frameTicks = Math.max(0, time - lastFrameTime);
		lastFrameTime = time;
		clipDistance = free < clipDistance ? free : clipDistance + (free - clipDistance) * Math.min(1, CLIP_RECOVERY * frameTicks);
		Vec3 orbit = clipFromAnchor(desired, clipDistance);

		if (time < PAN_IN) {
			if (dipIn)
				return time < DIP ? new Shot(eye, eyeYaw, eyePitch) : lookingAtMonument(orbit);
			return blend(eye, eyeYaw, eyePitch, orbit, ease(time / PAN_IN));
		}
		if (time < DURATION - PAN_OUT)
			return lookingAtMonument(orbit);

		if (dipOut == null)
			dipOut = isBlocked(orbit, eye);
		float t = (time - (DURATION - PAN_OUT)) / PAN_OUT;
		if (dipOut)
			return time < DURATION - PAN_OUT + DIP ? lookingAtMonument(orbit) : new Shot(eye, eyeYaw, eyePitch);
		Shot shot = blend(eye, eyeYaw, eyePitch, orbit, 1 - ease(t));
		// The return path follows the player as they move, so keep it out of anything they've put between
		return new Shot(clipBetween(eye, shot.position()), shot.yaw(), shot.pitch());
	}

	private static Vec3 orbitPosition(float time) {
		double angle = Math.toRadians(startAngle + SWEEP_DEGREES * time / DURATION);
		return new Vec3(center.x + Math.cos(angle) * radius, center.y + height, center.z + Math.sin(angle) * radius);
	}

	private static Shot lookingAtMonument(Vec3 position) {
		Vec3 d = lookTarget.subtract(position);
		float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new Shot(position, yaw, pitch);
	}

	// t=0 is the player's own view, t=1 is the sweep; the view turns towards the monument as the camera moves out
	private static Shot blend(Vec3 eye, float eyeYaw, float eyePitch, Vec3 orbit, float t) {
		Shot look = lookingAtMonument(eye.lerp(orbit, t));
		return new Shot(look.position(), eyeYaw + Mth.wrapDegrees(look.yaw() - eyeYaw) * t, Mth.lerp(t, eyePitch, look.pitch()));
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static Vec3 clipFromAnchor(Vec3 desired, double maxDistance) {
		Vec3 offset = desired.subtract(anchor);
		double distance = Math.min(offset.length(), maxDistance);
		return anchor.add(offset.normalize().scale(distance));
	}

	private static Vec3 clipBetween(Vec3 from, Vec3 to) {
		Vec3 offset = to.subtract(from);
		return from.add(offset.normalize().scale(Math.min(offset.length(), freeDistance(from, to))));
	}

	// How far from `from` towards `to` the camera can go before it would be inside a block
	private static double freeDistance(Vec3 from, Vec3 to) {
		Minecraft minecraft = Minecraft.getInstance();
		double length = from.distanceTo(to);
		HitResult hit = minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player));
		if (hit.getType() == HitResult.Type.MISS)
			return length;
		return Math.max(Math.min(MIN_DISTANCE, length), from.distanceTo(hit.getLocation()) - CLIP_MARGIN);
	}

	private static boolean isBlocked(Vec3 from, Vec3 to) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, minecraft.player)).getType() != HitResult.Type.MISS;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!active && !fading)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null) {
			active = false;
			fading = false;
			addedNightVision = false;
			return;
		}
		if (minecraft.isPaused())
			return;
		if (active && ++ticks >= DURATION)
			active = false;
		// Night vision is removed only once it has faded to nothing, so the lighting doesn't jump
		if (fading && ++fadeTicks >= DURATION + FADE_TAIL) {
			fading = false;
			if (addedNightVision)
				minecraft.player.getActiveEffectsMap().remove(MobEffects.NIGHT_VISION);
			addedNightVision = false;
		}
	}

	// Hide the HUD; the title and fades are drawn in onRenderGui instead
	@SubscribeEvent
	public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
		if (active)
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onRenderHand(RenderHandEvent event) {
		if (active)
			event.setCanceled(true);
	}

	// The blue underwater overlay is drawn when the player's eyes are in water, wherever the camera is
	@SubscribeEvent
	public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
		if (active && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER)
			event.setCanceled(true);
	}

	// Push the water fog out past the monument so the whole view is clear
	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		if (!fading)
			return;
		float renderDistance = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
		float strength = fadeStrength((float) event.getPartialTick());
		event.setNearPlaneDistance(Mth.lerp(strength, event.getNearPlaneDistance(), renderDistance * 0.8F));
		event.setFarPlaneDistance(Mth.lerp(strength, event.getFarPlaneDistance(), renderDistance));
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!active)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		GuiGraphicsExtractor graphics = event.getGuiGraphics();
		float time = ticks + event.getPartialTick().getGameTimeDeltaPartialTick(false);
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();

		// Letterbox bars slide in and out
		float bars = Math.min(ease(time / 20), ease((DURATION - time) / 20));
		int barHeight = (int) (height * 0.09F * bars);
		graphics.fill(0, 0, width, barHeight, 0xFF000000);
		graphics.fill(0, height - barHeight, width, height, 0xFF000000);

		// Title in the bottom-left corner
		float titleAlpha = Math.min(ease((time - 28) / 20), ease((DURATION - 34 - time) / 20));
		if (titleAlpha > 0.02F) {
			int alpha = (int) (titleAlpha * 255) << 24;
			float scale = 2.5F;
			int x = 18;
			int y = height - barHeight - 46;
			graphics.pose().pushMatrix();
			graphics.pose().translate(x, y);
			graphics.pose().scale(scale, scale);
			graphics.text(minecraft.font, "Ocean Monument", 0, 0, alpha | TITLE_COLOR, true);
			graphics.pose().popMatrix();
			int lineWidth = (int) (minecraft.font.width("Ocean Monument") * scale * ease((time - 28) / 30));
			graphics.fill(x, y + 25, x + lineWidth, y + 26, (int) (titleAlpha * 200) << 24 | TITLE_COLOR);
		}

		// Dips to black where the pan path is blocked
		float black = 0;
		if (dipIn && time < PAN_IN)
			black = time < DIP ? time / DIP : 1 - (time - DIP) / (PAN_IN - DIP);
		if (Boolean.TRUE.equals(dipOut) && time >= DURATION - PAN_OUT) {
			float t = time - (DURATION - PAN_OUT);
			black = t < DIP ? t / DIP : 1 - (t - DIP) / (PAN_OUT - DIP);
		}
		if (black > 0)
			graphics.fill(0, 0, width, height, (int) (Mth.clamp(black, 0, 1) * 255) << 24);
	}
}

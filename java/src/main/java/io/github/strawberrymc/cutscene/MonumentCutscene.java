package io.github.strawberrymc.cutscene;

import io.github.strawberrymc.MonumentIntro;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

// Cutscene played the first time a player enters an ocean monument (started by MonumentIntro): the spooky chime plays
// and the camera visits the monument in five close shots, cutting through black between them:
//  1. it pans out from the player to the entrance gate and pushes in towards it,
//  2. rises up a front corner, past the tiered walls,
//  3. circles the gazebo on the roof, with its hanging lump of Cracked Prismarine,
//  4. circles a Prismatic Sensor in the hall,
//  5. rises up the front of the monument to the gazebo while "Ocean Monument" appears in the corner,
// then dips to black and returns to the player. The client only draws chunks within its render distance of the player,
// which can be less than the monument is wide, so the corner, the sensor and the last shot's side are whichever are
// nearest the player, and every shot stays close to what it films. While it runs the HUD, hand, water overlay and water fog are hidden and night
// vision lights up the monument, which sits too deep for any daylight to reach.
//
// The camera never passes through blocks: each shot keeps a clear line of sight from a point it knows is open (in front
// of a wall, inside the gazebo, above the sensor), pulling in when something is in the way and easing back out once it
// clears. When the path from the player to the first shot is blocked (as it is from inside the monument), it dips to
// black and cuts instead of flying through walls. CutsceneCameraMixin applies the shot to the camera every frame.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class MonumentCutscene {
	private static final SoundEvent CHIME = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("strawberrymc", "monument_chime"));

	private static final int DURATION = MonumentIntro.CUTSCENE_TICKS;
	private static final int PAN_IN = 30;
	// When each shot starts; the last one runs until the final dip back to the player
	private static final int[] SHOTS = {0, 85, 160, 240, 315};
	// Half of a dip to black, used between shots and wherever the pan path is blocked
	private static final int DIP = 10;
	// Half of the slower dip to black at the end, back to the player
	private static final int END_DIP = 25;
	// How far the camera stays from whatever is blocking its view
	private static final double CLIP_MARGIN = 1.0;
	private static final double MIN_DISTANCE = 3.0;
	// Fraction of the remaining distance regained per tick once an obstruction clears
	private static final double CLIP_RECOVERY = 0.12;
	// Night vision and the cleared fog fade in at the start, then fade out from the start of the final dip to this many ticks after
	// the cutscene
	private static final int FADE_IN = 20;
	private static final int FADE_TAIL = 40;
	private static final int TITLE_COLOR = 0xD8F4FF;

	public record Shot(Vec3 position, float yaw, float pitch) {
	}

	// Where a shot wants the camera, the open point its line of sight is checked from, and what it looks at
	private record Framing(Vec3 anchor, Vec3 desired, Vec3 target) {
	}

	private static boolean active;
	private static int ticks;
	// The monument's centre at floor level, half its width, and its entrance's outward and sideways directions
	private static Vec3 center;
	private static double halfWidth;
	private static double shellHeight;
	private static Vec3 entranceNormal;
	private static Vec3 side;
	// Horizontal direction from the monument's centre to the player, so shots favour the side whose chunks are loaded
	private static Vec3 towardPlayer;
	private static Vec3 vent;
	private static Vec3 sensor;
	private static boolean dipIn;
	private static int lastShot;
	private static double clipDistance;
	private static float lastFrameTime;
	private static boolean addedNightVision;
	// Night vision and fog keep fading after the cutscene ends
	private static boolean fading;
	private static int fadeTicks;

	public static void start(BlockPos min, BlockPos max, Direction entrance, BlockPos ventPos, BlockPos sensorPos) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || active)
			return;
		center = new Vec3((min.getX() + max.getX() + 1) / 2.0, min.getY(), (min.getZ() + max.getZ() + 1) / 2.0);
		halfWidth = Math.max(max.getX() - min.getX() + 1, max.getZ() - min.getZ() + 1) / 2.0;
		vent = Vec3.atBottomCenterOf(ventPos.above());
		shellHeight = vent.y - center.y;
		sensor = Vec3.atCenterOf(sensorPos);
		entranceNormal = new Vec3(entrance.getStepX(), 0, entrance.getStepZ());
		side = new Vec3(-entranceNormal.z, 0, entranceNormal.x);
		towardPlayer = new Vec3(player.getX() - center.x, 0, player.getZ() - center.z);
		towardPlayer = towardPlayer.lengthSqr() < 0.01 ? entranceNormal : towardPlayer.normalize();

		active = true;
		ticks = 0;
		lastFrameTime = 0;
		lastShot = 0;
		clipDistance = Double.MAX_VALUE;
		Framing first = framing(0, 0);
		dipIn = isBlocked(player.getEyePosition(), clipFrom(first.anchor(), first.desired(), Double.MAX_VALUE));

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
		int fadeOutStart = DURATION - 2 * END_DIP;
		return Math.min(ease(time / FADE_IN), 1 - ease((time - fadeOutStart) / (2 * END_DIP + FADE_TAIL)));
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
		// The last dip to black cuts back to the player
		if (time >= DURATION - END_DIP)
			return new Shot(eye, eyeYaw, eyePitch);

		int shot = SHOTS.length - 1;
		while (time < SHOTS[shot])
			shot--;
		if (shot != lastShot) {
			lastShot = shot;
			clipDistance = Double.MAX_VALUE;
		}
		int end = shot + 1 < SHOTS.length ? SHOTS[shot + 1] : DURATION - END_DIP;
		Framing framing = framing(shot, (time - SHOTS[shot]) / (end - SHOTS[shot]));

		// The line of sight shrinks instantly when blocked and grows back smoothly
		double free = freeDistance(framing.anchor(), framing.desired());
		double frameTicks = Math.max(0, time - lastFrameTime);
		lastFrameTime = time;
		clipDistance = free < clipDistance ? free : clipDistance + (free - clipDistance) * Math.min(1, CLIP_RECOVERY * frameTicks);
		Shot view = lookingAt(clipFrom(framing.anchor(), framing.desired(), clipDistance), framing.target());

		if (shot == 0 && time < PAN_IN) {
			if (dipIn)
				return time < DIP ? new Shot(eye, eyeYaw, eyePitch) : view;
			return blend(eye, eyeYaw, eyePitch, view.position(), framing.target(), ease(time / PAN_IN));
		}
		return view;
	}

	private static Framing framing(int shot, float progress) {
		float p = ease(progress);
		switch (shot) {
			case 0 -> {
				// The entrance gate, pushing in
				Vec3 gate = center.add(entranceNormal.scale(halfWidth)).add(0, 8, 0);
				Vec3 desired = gate.add(entranceNormal.scale(Mth.lerp(p, 30, 17))).add(0, Mth.lerp(p, 8, 3), 0);
				return new Framing(gate.add(entranceNormal.scale(3)).add(0, 4, 0), desired, gate.subtract(entranceNormal.scale(6)).subtract(0, 1, 0));
			}
			case 1 -> {
				// The corner nearest the player, rising while looking in across the tiered walls towards the middle
				Vec3 out = diagonal(towardPlayer, 0);
				Vec3 corner = center.add(out.scale(halfWidth * Math.sqrt(2)));
				Vec3 across = new Vec3(-out.z, 0, out.x);
				double height = Mth.lerp(p, 6, shellHeight * 0.55);
				Vec3 desired = corner.add(out.scale(14)).add(across.scale(Mth.lerp(p, -5, 5))).add(0, height, 0);
				return new Framing(corner.add(out.scale(3)).add(0, height, 0), desired, corner.subtract(out.scale(34)).add(0, height * 0.8, 0));
			}
			case 2 -> {
				// Around the gazebo, between its pillars, looking up at the dome under its roof
				double base = Math.toDegrees(Math.atan2(towardPlayer.z, towardPlayer.x)) - 25;
				double angle = Math.toRadians(base + 50 * p);
				Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
				Vec3 dome = vent.add(0, 5, 0);
				return new Framing(vent.add(direction.scale(2.5)).add(0, 1.2, 0), vent.add(direction.scale(18)).add(0, 3, 0), dome.subtract(0, 1.5, 0));
			}
			case 3 -> {
				// Around a Prismatic Sensor, from the hall side
				Vec3 inward = new Vec3(center.x - sensor.x, 0, center.z - sensor.z).normalize();
				double angle = Math.atan2(inward.z, inward.x) + Math.toRadians(Mth.lerp(p, -25, 25));
				Vec3 direction = new Vec3(Math.cos(angle), 0, Math.sin(angle));
				Vec3 above = sensor.add(0, 1.2, 0);
				return new Framing(above, sensor.add(direction.scale(Mth.lerp(p, 6, 4.5))).add(0, 1.5, 0), sensor.subtract(0, 0.2, 0));
			}
			default -> {
				// Rising over the corner of the roof nearest the player with the gazebo ahead, for the title
				Vec3 diagonal = diagonal(towardPlayer, 0);
				double height = Mth.lerp(p, shellHeight * 0.7, shellHeight + 16);
				Vec3 desired = center.add(diagonal.scale(46)).add(0, height, 0);
				Vec3 anchor = center.add(diagonal.scale(24)).add(0, shellHeight + 4, 0);
				return new Framing(anchor, desired, vent.add(0, 2, 0));
			}
		}
	}

	// The monument's diagonal (45 degrees from its walls) nearest a direction
	private static Vec3 diagonal(Vec3 direction, double offsetDegrees) {
		double angle = Math.toDegrees(Math.atan2(direction.z, direction.x));
		angle = Math.toRadians(Math.round((angle - 45) / 90) * 90 + 45 + offsetDegrees);
		return new Vec3(Math.cos(angle), 0, Math.sin(angle));
	}

	private static Shot lookingAt(Vec3 position, Vec3 target) {
		Vec3 d = target.subtract(position);
		float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new Shot(position, yaw, pitch);
	}

	// t=0 is the player's own view, t=1 is the first shot; the view turns towards the target as the camera moves out
	private static Shot blend(Vec3 eye, float eyeYaw, float eyePitch, Vec3 to, Vec3 target, float t) {
		Shot look = lookingAt(eye.lerp(to, t), target);
		return new Shot(look.position(), eyeYaw + Mth.wrapDegrees(look.yaw() - eyeYaw) * t, Mth.lerp(t, eyePitch, look.pitch()));
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static Vec3 clipFrom(Vec3 from, Vec3 desired, double maxDistance) {
		Vec3 offset = desired.subtract(from);
		double distance = Math.min(offset.length(), maxDistance);
		return from.add(offset.normalize().scale(distance));
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
		float titleStart = SHOTS[SHOTS.length - 1] + 15;
		float titleAlpha = Math.min(ease((time - titleStart) / 20), ease((DURATION - END_DIP - 5 - time) / 15));
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
			int lineWidth = (int) (minecraft.font.width("Ocean Monument") * scale * ease((time - titleStart) / 30));
			graphics.fill(x, y + 25, x + lineWidth, y + 26, (int) (titleAlpha * 200) << 24 | TITLE_COLOR);
		}

		// Dips to black between scenes, back to the player, and where the pan path is blocked
		float black = 0;
		if (dipIn && time < PAN_IN)
			black = time < DIP ? time / DIP : 1 - (time - DIP) / (PAN_IN - DIP);
		for (int i = 1; i < SHOTS.length; i++) {
			int cut = SHOTS[i];
			black = Math.max(black, 1 - Math.abs(time - cut) / DIP);
		}
		black = Math.max(black, 1 - Math.abs(time - (DURATION - END_DIP)) / END_DIP);
		if (black > 0)
			graphics.fill(0, 0, width, height, (int) (Mth.clamp(black, 0, 1) * 255) << 24);
	}
}

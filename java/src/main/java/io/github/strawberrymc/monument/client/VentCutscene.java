package io.github.strawberrymc.monument.client;

import io.github.strawberrymc.monument.PrismaticSensors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
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

import io.github.strawberrymc.cutscene.MonumentCutscene;

import javax.annotation.Nullable;

// Played when the Ancient Prismarine capping the monument's roof vent is mined (PrismaticSensors.checkVent): the camera
// pulls back from the player to a side view of the gazebo, watches the water jet rise out of the vent and shatter the
// Cracked Prismarine dome on the gazebo's ceiling, then returns. The shot keeps a clear line of sight from inside the gazebo, and dips to black
// instead of passing through walls when the way out is blocked. CutsceneCameraMixin applies the shot.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class VentCutscene {
	private static final int DURATION = PrismaticSensors.VENT_CUTSCENE_TICKS;
	private static final int PAN = 22;
	private static final int DIP = 8;
	// How far back and up from the vent the camera watches from
	private static final double DISTANCE = 15;
	private static final double HEIGHT = 0.5;
	private static final double CLIP_MARGIN = 0.8;

	private static boolean active;
	private static int ticks;
	private static Vec3 shot;
	private static Vec3 target;
	private static boolean dip;

	public static void start(BlockPos vent, BlockPos lump) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || active || MonumentCutscene.isActive())
			return;
		Vec3 ventTop = Vec3.atBottomCenterOf(vent.above());
		target = ventTop.lerp(Vec3.atCenterOf(lump), 0.5);
		// Back away from the vent on the side the player is on, so the camera "moves back" from them
		Vec3 away = new Vec3(player.getX() - ventTop.x, 0, player.getZ() - ventTop.z);
		if (away.lengthSqr() < 0.01)
			away = Vec3.directionFromRotation(0, player.getYRot() + 180);
		// The gazebo's pillars stand every 45 degrees around the vent, so look in between two of them
		double angle = Math.toDegrees(Math.atan2(away.z, away.x));
		angle = Math.toRadians(Math.round((angle - 22.5) / 45) * 45 + 22.5);
		away = new Vec3(Math.cos(angle), 0, Math.sin(angle));
		Vec3 desired = target.add(away.scale(DISTANCE)).add(0, HEIGHT, 0);
		// Keep a clear line of sight from just above the vent
		Vec3 anchor = ventTop.add(0, 1.5, 0);
		HitResult hit = minecraft.level.clip(new ClipContext(anchor, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
		shot = hit.getType() == HitResult.Type.MISS ? desired
			: anchor.add(desired.subtract(anchor).normalize().scale(Math.max(2.5, anchor.distanceTo(hit.getLocation()) - CLIP_MARGIN)));
		dip = minecraft.level.clip(new ClipContext(player.getEyePosition(), shot, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType()
			!= HitResult.Type.MISS;
		active = true;
		ticks = 0;
	}

	public static boolean isActive() {
		return active;
	}

	public static @Nullable MonumentCutscene.Shot currentShot(float partialTicks) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!active || player == null)
			return null;
		float time = Math.min(ticks + partialTicks, DURATION);
		Vec3 eye = player.getEyePosition(partialTicks);
		MonumentCutscene.Shot eyeShot = new MonumentCutscene.Shot(eye, player.getViewYRot(partialTicks), player.getViewXRot(partialTicks));
		MonumentCutscene.Shot held = looking(shot);
		float t;
		if (time < PAN)
			t = ease(time / PAN);
		else if (time > DURATION - PAN)
			t = 1 - ease((time - (DURATION - PAN)) / PAN);
		else
			return held;
		if (dip)
			return t > 0.5F ? held : eyeShot;
		Vec3 position = eye.lerp(shot, t);
		MonumentCutscene.Shot look = looking(position);
		return new MonumentCutscene.Shot(position, eyeShot.yaw() + Mth.wrapDegrees(look.yaw() - eyeShot.yaw()) * t, Mth.lerp(t, eyeShot.pitch(), look.pitch()));
	}

	private static MonumentCutscene.Shot looking(Vec3 position) {
		Vec3 d = target.subtract(position);
		float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new MonumentCutscene.Shot(position, yaw, pitch);
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!active)
			return;
		if (minecraft.player == null || minecraft.level == null) {
			active = false;
			return;
		}
		if (!minecraft.isPaused() && ++ticks >= DURATION)
			active = false;
	}

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

	@SubscribeEvent
	public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
		if (active && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER)
			event.setCanceled(true);
	}

	// Push the water fog back far enough to see the whole gazebo
	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		if (!active)
			return;
		float time = ticks + (float) event.getPartialTick();
		float strength = Math.min(ease(time / PAN), ease((DURATION - time) / PAN));
		event.setNearPlaneDistance(Mth.lerp(strength, event.getNearPlaneDistance(), 24));
		event.setFarPlaneDistance(Mth.lerp(strength, event.getFarPlaneDistance(), 64));
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!active)
			return;
		GuiGraphicsExtractor graphics = event.getGuiGraphics();
		float time = ticks + event.getPartialTick().getGameTimeDeltaPartialTick(false);
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		float bars = Math.min(ease(time / 15), ease((DURATION - time) / 15));
		int barHeight = (int) (height * 0.09F * bars);
		graphics.fill(0, 0, width, barHeight, 0xFF000000);
		graphics.fill(0, height - barHeight, width, height, 0xFF000000);
		if (dip) {
			// Black around the cut in and the cut out
			float black = Math.max(1 - Math.abs(time - PAN / 2F) / DIP, 1 - Math.abs(time - (DURATION - PAN / 2F)) / DIP);
			if (black > 0)
				graphics.fill(0, 0, width, height, (int) (Mth.clamp(black, 0, 1) * 255) << 24);
		}
	}
}

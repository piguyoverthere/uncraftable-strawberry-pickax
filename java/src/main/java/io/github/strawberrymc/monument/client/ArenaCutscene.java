package io.github.strawberrymc.monument.client;

import io.github.strawberrymc.cutscene.MonumentCutscene;
import io.github.strawberrymc.monument.MonumentArena;
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

import javax.annotation.Nullable;

// Played when a Monument Key goes into the arena's Prismatic Vault (MonumentArena). The player first sees the rune swirl
// start above the glass, then the scene cuts to inside the tank: the camera watches the glyphs spiral down into the
// Prismatic Rune's item frame, the rune glow, break loose and rise to the surface, slowly circling it all the while, and
// finally cuts back to the player. CutsceneCameraMixin applies the shot.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class ArenaCutscene {
	private static final int DURATION = MonumentArena.CUTSCENE_TICKS;
	// When the scene cuts into the tank, and when it cuts back to the player
	private static final int CUT_IN = 30;
	private static final int CUT_OUT = DURATION - 12;
	private static final int DIP = 8;
	private static final double DISTANCE = 5;
	private static final double CLIP_MARGIN = 0.6;

	private static boolean active;
	private static int ticks;
	private static Vec3 frame;
	private static double surfaceY;
	private static double startAngle;

	public static void start(BlockPos framePos, BlockPos vaultPos) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || active || MonumentCutscene.isActive() || VentCutscene.isActive())
			return;
		frame = Vec3.atBottomCenterOf(framePos);
		surfaceY = vaultPos.getY() - 0.6;
		// Look in from the side the player is standing on
		startAngle = Math.atan2(player.getZ() - frame.z, player.getX() - frame.x);
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
		// Before the cut in and after the cut out it's the player's own view
		if (time < CUT_IN || time >= CUT_OUT)
			return null;

		// How far the rune has risen so far
		double rise = 0;
		if (time > MonumentArena.RELEASE)
			rise = ease((time - MonumentArena.RELEASE) / MonumentArena.RISE) * (surfaceY - frame.y);
		// Look up at the glyphs coming down, settle on the frame as it glows, then follow the rune up
		double targetY;
		if (time < MonumentArena.GLOW)
			targetY = frame.y + 2.4 - 1.8 * ease((time - CUT_IN) / (MonumentArena.GLOW - CUT_IN));
		else
			targetY = frame.y + 0.6 + rise;
		Vec3 target = new Vec3(frame.x, targetY, frame.z);

		double angle = startAngle + Math.toRadians(40) * (time - CUT_IN) / (CUT_OUT - CUT_IN);
		Vec3 desired = new Vec3(frame.x + Math.cos(angle) * DISTANCE, frame.y + 1.0 + rise * 0.6, frame.z + Math.sin(angle) * DISTANCE);
		// Keep a clear line of sight from just above the frame
		Vec3 anchor = new Vec3(frame.x, desired.y, frame.z).add(0, 0.5, 0);
		Minecraft minecraft = Minecraft.getInstance();
		HitResult hit = minecraft.level.clip(new ClipContext(anchor, desired, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
		Vec3 position = hit.getType() == HitResult.Type.MISS ? desired
			: anchor.add(desired.subtract(anchor).normalize().scale(Math.max(1.5, anchor.distanceTo(hit.getLocation()) - CLIP_MARGIN)));

		Vec3 d = target.subtract(position);
		float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90;
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
		return new MonumentCutscene.Shot(position, yaw, pitch);
	}

	private static float ease(float t) {
		t = Mth.clamp(t, 0, 1);
		return t * t * (3 - 2 * t);
	}

	private static double ease(double t) {
		return ease((float) t);
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
		if (active && ticks >= CUT_IN && ticks < CUT_OUT)
			event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onScreenEffect(RenderBlockScreenEffectEvent event) {
		if (active && event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.WATER)
			event.setCanceled(true);
	}

	// Clear the water inside the tank so the whole funnel shows
	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		if (!active || ticks < CUT_IN || ticks >= CUT_OUT)
			return;
		event.setNearPlaneDistance(Math.max(event.getNearPlaneDistance(), 16));
		event.setFarPlaneDistance(Math.max(event.getFarPlaneDistance(), 40));
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
		// Black around the cut into the tank and the cut back out
		float black = Math.max(1 - Math.abs(time - CUT_IN) / DIP, 1 - Math.abs(time - CUT_OUT) / DIP);
		if (black > 0)
			graphics.fill(0, 0, width, height, (int) (Mth.clamp(black, 0, 1) * 255) << 24);
	}
}

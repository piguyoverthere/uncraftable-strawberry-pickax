package io.github.strawberrymc.monument.client;

import io.github.strawberrymc.cutscene.MonumentCutscene;
import io.github.strawberrymc.monument.MonumentArena;
import io.github.strawberrymc.monument.MonumentQuest;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector3fc;

import java.util.List;

// Draws the player's monument objective and its pings. The objective sits in a small panel on the left side of the screen,
// which slides in when a new objective arrives (after the entrance cutscene for the first one). Pings are drawn on the
// HUD at their world position, so they show through blocks: a flat single-colour diamond ring around a diamond core,
// blinking on and off, with the label and distance underneath. A ping that's off screen sits on the screen's edge instead, pointing the way.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class QuestHud {
	private static final int PANEL = 0xB0101826;

	private static String objective = "";
	private static List<MonumentQuest.Ping> pings = List.of();
	private static long announcedAt = Long.MIN_VALUE;
	private static String notice = "";
	private static long noticeAt = Long.MIN_VALUE;

	public static void receive(MonumentQuest.SyncQuest sync) {
		objective = sync.objective();
		pings = sync.pings();
		if (sync.announce() && !objective.isEmpty())
			announcedAt = System.currentTimeMillis();
		if (!sync.notice().isEmpty()) {
			notice = sync.notice();
			noticeAt = System.currentTimeMillis();
		}
	}

	@SubscribeEvent
	public static void registerProviders(RegisterParticleProvidersEvent event) {
		event.registerSpriteSet(MonumentArena.RUNE_FUNNEL.get(), RuneFunnel.Provider::new);
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui || MonumentCutscene.isActive() || VentCutscene.isActive() || ArenaCutscene.isActive())
			return;
		GuiGraphicsExtractor graphics = event.getGuiGraphics();
		float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		for (MonumentQuest.Ping ping : pings)
			drawPing(graphics, minecraft, ping, partialTick);
		if (!objective.isEmpty())
			drawObjective(graphics, minecraft.font);
		drawNotice(graphics, minecraft.font);
	}

	// A line of story in the lower middle of the screen, fading in and out
	private static void drawNotice(GuiGraphicsExtractor graphics, Font font) {
		float seconds = (System.currentTimeMillis() - noticeAt) / 1000F;
		float length = MonumentQuest.NOTICE_TICKS / 20F;
		float alpha = Math.min(Math.min(seconds / 0.6F, (length - seconds) / 0.8F), 1);
		if (alpha <= 0.02F)
			return;
		String[] lines = notice.split("\n");
		int y = (int) (graphics.guiHeight() * 0.62F);
		int width = 0;
		for (String line : lines)
			width = Math.max(width, font.width(line));
		int centerX = graphics.guiWidth() / 2;
		graphics.fill(centerX - width / 2 - 8, y - 5, centerX + width / 2 + 8, y + lines.length * 12 + 2, (int) (alpha * 0x90) << 24 | 0x101826);
		for (int i = 0; i < lines.length; i++)
			graphics.text(font, net.minecraft.network.chat.Component.literal(lines[i]).withStyle(net.minecraft.ChatFormatting.ITALIC),
				centerX - font.width(lines[i]) / 2, y + i * 12, (int) (alpha * 255) << 24 | (i == 0 ? 0xFFFFFF : MonumentQuest.ACCENT_COLOR), true);
	}

	// A small panel on the left side of the screen. A new objective slides it in from the edge
	private static void drawObjective(GuiGraphicsExtractor graphics, Font font) {
		float seconds = (System.currentTimeMillis() - announcedAt) / 1000F;
		float slide = seconds < 0.5F ? 1 - (float) Math.pow(1 - Math.max(seconds, 0) / 0.5F, 3) : 1;
		float scale = 0.75F;
		int width = (int) ((Math.max(font.width(objective), font.width("Objective")) + 14) * scale);
		int height = (int) (28 * scale);
		int y = graphics.guiHeight() / 3;
		graphics.pose().pushMatrix();
		graphics.pose().translate((width + 6) * (slide - 1), y);
		graphics.fill(0, 0, width + 4, height, PANEL);
		graphics.fill(0, 0, 2, height, 0xFF000000 | MonumentQuest.OBJECTIVE_COLOR);
		graphics.pose().scale(scale, scale);
		graphics.text(font, "Objective", 8, 4, 0xFF000000 | MonumentQuest.ACCENT_COLOR, false);
		graphics.text(font, objective, 8, 16, 0xFFFFFFFF, false);
		graphics.pose().popMatrix();
	}

	private static void drawPing(GuiGraphicsExtractor graphics, Minecraft minecraft, MonumentQuest.Ping ping, float partialTick) {
		Camera camera = minecraft.gameRenderer.getMainCamera();
		Vec3 offset = ping.pos().subtract(camera.position());
		Vector3fc forward = camera.forwardVector(), up = camera.upVector(), left = camera.leftVector();
		double depth = offset.x * forward.x() + offset.y * forward.y() + offset.z * forward.z();
		double right = -(offset.x * left.x() + offset.y * left.y() + offset.z * left.z());
		double upward = offset.x * up.x() + offset.y * up.y() + offset.z * up.z();
		int width = graphics.guiWidth(), height = graphics.guiHeight();
		double focal = (height / 2.0) / Math.tan(Math.toRadians(camera.getFov()) / 2);
		double x, y;
		boolean onScreen = depth > 0.05;
		if (onScreen) {
			x = width / 2.0 + right / depth * focal;
			y = height / 2.0 - upward / depth * focal;
		} else {
			// Behind the camera: push it out towards the matching edge
			x = width / 2.0 + right * 1000;
			y = height / 2.0 - upward * 1000;
		}
		int margin = 22;
		boolean clamped = x < margin || x > width - margin || y < margin || y > height - margin || !onScreen;
		if (clamped) {
			double dx = x - width / 2.0, dy = y - height / 2.0;
			double scale = Math.min((width / 2.0 - margin) / Math.max(Math.abs(dx), 1e-6), (height / 2.0 - margin) / Math.max(Math.abs(dy), 1e-6));
			x = width / 2.0 + dx * Math.min(scale, 1);
			y = height / 2.0 + dy * Math.min(scale, 1);
		}

		// Flat, one colour: a diamond ring around a solid diamond core. Blinking turns it hard on and off
		float time = (minecraft.player.tickCount + partialTick) / 20F;
		if (ping.blink() && time % 1.2F > 0.85F)
			return;
		float size = 7 * ping.size();
		int color = 0xFF000000 | ping.color();
		graphics.pose().pushMatrix();
		graphics.pose().translate((float) x, (float) y);
		drawDiamondFrame(graphics, size * 1.4F, 1.5F, color, 45);
		drawFilledDiamond(graphics, size * 0.6F, color);
		graphics.pose().popMatrix();

		// Label and distance under it, in the same colour
		Font font = minecraft.font;
		String distance = (int) Math.round(Math.sqrt(offset.lengthSqr())) + "m";
		String text = ping.label().isEmpty() ? distance : ping.label() + "  " + distance;
		int textY = (int) (y + size * 1.6F);
		if (clamped && textY > height - 12)
			textY = (int) (y - size * 1.6F - 9);
		int textX = Mth.clamp((int) x - font.width(text) / 2, 2, width - font.width(text) - 2);
		graphics.text(font, text, textX, textY, color, true);
	}

	private static void drawDiamondFrame(GuiGraphicsExtractor graphics, float half, float thickness, int color, float degrees) {
		graphics.pose().pushMatrix();
		graphics.pose().rotate((float) Math.toRadians(degrees));
		float side = half / (float) Math.sqrt(2);
		fill(graphics, -side, -side, side, -side + thickness, color);
		fill(graphics, -side, side - thickness, side, side, color);
		fill(graphics, -side, -side + thickness, -side + thickness, side - thickness, color);
		fill(graphics, side - thickness, -side + thickness, side, side - thickness, color);
		graphics.pose().popMatrix();
	}

	private static void drawFilledDiamond(GuiGraphicsExtractor graphics, float half, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().rotate((float) Math.PI / 4);
		float side = half / (float) Math.sqrt(2);
		fill(graphics, -side, -side, side, side, color);
		graphics.pose().popMatrix();
	}

	// fill() takes whole numbers, so scale up to draw fractional rectangles
	private static void fill(GuiGraphicsExtractor graphics, float x0, float y0, float x1, float y1, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().scale(0.25F, 0.25F);
		graphics.fill(Math.round(x0 * 4), Math.round(y0 * 4), Math.round(x1 * 4), Math.round(y1 * 4), color);
		graphics.pose().popMatrix();
	}

	// Rune glyphs that spiral down into the point given as their velocity, brightening as they go
	public static class RuneFunnel extends SingleQuadParticle {
		private final double targetX, targetY, targetZ, startY, radius, startAngle;

		RuneFunnel(ClientLevel level, double x, double y, double z, double targetX, double targetY, double targetZ, TextureAtlasSprite sprite) {
			super(level, x, y, z, sprite);
			this.targetX = targetX;
			this.targetY = targetY;
			this.targetZ = targetZ;
			this.startY = y;
			this.radius = Math.sqrt((x - targetX) * (x - targetX) + (z - targetZ) * (z - targetZ));
			this.startAngle = Math.atan2(z - targetZ, x - targetX);
			this.lifetime = 40 + random.nextInt(15);
			this.hasPhysics = false;
			this.gravity = 0;
			this.quadSize = 0.12F + random.nextFloat() * 0.06F;
			this.rCol = 0.2F;
			this.gCol = 0.55F;
			this.bCol = 1.0F;
			this.setAlpha(0);
		}

		@Override
		public SingleQuadParticle.Layer getLayer() {
			return SingleQuadParticle.Layer.TRANSLUCENT;
		}

		@Override
		public int getLightCoords(float a) {
			return LightCoordsUtil.withBlock(super.getLightCoords(a), 15);
		}

		@Override
		public void tick() {
			xo = x;
			yo = y;
			zo = z;
			if (age++ >= lifetime) {
				remove();
				return;
			}
			float p = (float) age / lifetime;
			double r = radius * (1 - p * p * (3 - 2 * p));
			double angle = startAngle + p * Math.PI * 2.5;
			x = targetX + Math.cos(angle) * r;
			z = targetZ + Math.sin(angle) * r;
			y = Mth.lerp(Math.pow(p, 1.4), startY, targetY);
			this.rCol = 0.2F + 0.6F * p;
			this.gCol = 0.55F + 0.4F * p;
			setAlpha(Math.min(1, Math.min(p / 0.15F, (1 - p) / 0.1F)));
		}

		public static class Provider implements ParticleProvider<SimpleParticleType> {
			private final SpriteSet sprites;

			public Provider(SpriteSet sprites) {
				this.sprites = sprites;
			}

			@Override
			public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double tx, double ty, double tz,
					RandomSource random) {
				return new RuneFunnel(level, x, y, z, tx, ty, tz, sprites.get(random));
			}
		}
	}
}

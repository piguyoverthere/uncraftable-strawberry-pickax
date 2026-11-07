package io.github.strawberrymc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

// The Crystal Hollows cavern is so open that you can see out to the edge of the loaded chunks, where the sky shows
// through. Inside the biome the fog closes in well before the render distance and covers the sky, so the cavern fades
// into its dark fog colour (set on the biome element in MCreator) instead.
@EventBusSubscriber(modid = "strawberrymc", value = Dist.CLIENT)
public class CrystalHollowsFog {
	private static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("strawberrymc", "crystal_hollows"));
	private static final float FOG_START = 20;
	private static final float FOG_END = 72;

	@SubscribeEvent
	public static void onRenderFog(ViewportEvent.RenderFog event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || event.getType() != FogType.ATMOSPHERIC)
			return;
		if (!minecraft.level.getBiome(event.getCamera().blockPosition()).is(BIOME))
			return;
		FogData fog = event.getFogData();
		fog.environmentalStart = Math.min(fog.environmentalStart, FOG_START);
		fog.environmentalEnd = Math.min(fog.environmentalEnd, FOG_END);
		fog.renderDistanceStart = Math.min(fog.renderDistanceStart, FOG_START);
		fog.renderDistanceEnd = Math.min(fog.renderDistanceEnd, FOG_END);
		// No sky or clouds show through gaps in the cavern; they're drawn in the fog colour
		fog.skyEnd = 0;
		fog.cloudEnd = 0;
	}
}

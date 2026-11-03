package io.github.strawberrymc.custommixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

// Lets ComScript's getTemperature() read a biome's height-adjusted temperature, the value that decides rain or snow
@Mixin(Biome.class)
public interface BiomeTemperatureInvoker {
	@Invoker("getTemperature")
	float strawberrymc$getTemperature(BlockPos pos, int seaLevel);
}

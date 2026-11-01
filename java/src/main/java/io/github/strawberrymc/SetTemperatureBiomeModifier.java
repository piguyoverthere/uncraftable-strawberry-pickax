package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

// Biome modifier "strawberrymc:set_temperature" that overrides a biome's temperature. Below 0.15 a biome gets
// snow layers and snowfall, so this keeps Alpine Meadow snow-free (see biome_modifier/alpine_meadow_no_snow.json)
// without touching MCreator's generated biome, which it would overwrite.
public record SetTemperatureBiomeModifier(HolderSet<Biome> biomes, float temperature) implements BiomeModifier {
	public static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
		DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, "strawberrymc");

	public static final Supplier<MapCodec<SetTemperatureBiomeModifier>> CODEC = SERIALIZERS.register("set_temperature",
		() -> RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("biomes").forGetter(SetTemperatureBiomeModifier::biomes),
			Codec.FLOAT.fieldOf("temperature").forGetter(SetTemperatureBiomeModifier::temperature)
		).apply(instance, SetTemperatureBiomeModifier::new)));

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
		if (phase == Phase.MODIFY && this.biomes.contains(biome))
			builder.getClimateSettings().setTemperature(this.temperature);
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec() {
		return CODEC.get();
	}

	public static void init(IEventBus eventBus) {
		SERIALIZERS.register(eventBus);
	}
}

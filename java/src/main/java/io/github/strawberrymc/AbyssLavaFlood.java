package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

// The Ocean Abyss floor sits around y=-56, but below y=-54 worldgen fills open space with lava instead of water, which
// would leave a layer of lava under the ocean, and lava springs can open into the water from the Abyss walls. This
// turns all lava below sea level into water in Ocean Abyss columns, once per chunk after every other feature
// (added to the Abyss by biome_modifier/abyss_lava_flood.json).
public class AbyssLavaFlood extends Feature<NoneFeatureConfiguration> {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "strawberrymc");

	public static final Supplier<Feature<NoneFeatureConfiguration>> ABYSS_LAVA_FLOOD =
		FEATURES.register("abyss_lava_flood", () -> new AbyssLavaFlood(NoneFeatureConfiguration.CODEC));

	private static final ResourceKey<Biome> OCEAN_ABYSS = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("strawberrymc", "ocean_abyss"));
	// Biome is checked at the Abyss floor depth
	private static final int BIOME_Y = -54;

	public AbyssLavaFlood(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		WorldGenLevel level = context.level();
		int minX = context.origin().getX() & ~15;
		int minZ = context.origin().getZ() & ~15;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int seaLevel = level.getSeaLevel();
		boolean changed = false;
		for (int x = minX; x < minX + 16; x++) {
			for (int z = minZ; z < minZ + 16; z++) {
				if (!level.getBiome(pos.set(x, BIOME_Y, z)).is(OCEAN_ABYSS))
					continue;
				for (int y = level.getMinY(); y < seaLevel; y++) {
					pos.set(x, y, z);
					if (level.getBlockState(pos).is(Blocks.LAVA)) {
						level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	public static void init(IEventBus eventBus) {
		FEATURES.register(eventBus);
	}
}

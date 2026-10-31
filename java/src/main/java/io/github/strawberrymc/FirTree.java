package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.strawberrymc.procedures.DetermineRandFirProcedure;

import java.util.function.Supplier;

// Worldgen feature that grows a random fir (alive, dead or snag) the same way MasterFirSpawner does.
// Spawner blocks can't be used directly in worldgen because onPlace doesn't fire there.
public class FirTree extends Feature<NoneFeatureConfiguration> {
    public static final DeferredRegister<Feature<?>> FEATURES =
        DeferredRegister.create(Registries.FEATURE, "strawberrymc");

    public static final Supplier<Feature<NoneFeatureConfiguration>> FIR_TREE =
        FEATURES.register("fir_tree", () -> new FirTree(NoneFeatureConfiguration.CODEC));

    public FirTree(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        // Origin is the first air block above the surface; the trunk replaces the ground block like the spawners do
        BlockPos ground = context.origin().below();

        // substrate_overworld covers grass, dirt, podzol, moss and mud (DIRT alone no longer includes grass)
        if (!level.getBlockState(ground).is(BlockTags.SUBSTRATE_OVERWORLD)) {
            return false;
        }

        DetermineRandFirProcedure.execute(level, ground.getX(), ground.getY(), ground.getZ());
        return true;
    }

    public static void init(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}

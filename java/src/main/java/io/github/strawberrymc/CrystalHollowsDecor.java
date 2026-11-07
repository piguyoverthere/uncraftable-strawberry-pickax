package io.github.strawberrymc;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.github.strawberrymc.init.StrawberrymcModBlocks;
import io.github.strawberrymc.procedures.CalciteMountainGenerationProcedure;
import io.github.strawberrymc.procedures.CrystalColumnGenerationProcedure;

import java.util.function.Supplier;

// Worldgen features that run the Crystal Hollows MCreator procedures, the same way FirTree runs the fir procedures.
// Their placed features (placed_feature/crystal_column.json, calcite_mountain.json) scan down to an air block
// standing on the cavern floor and hand that position to the procedure.
public class CrystalHollowsDecor {
	public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "strawberrymc");

	public static final Supplier<Feature<NoneFeatureConfiguration>> CRYSTAL_COLUMN =
		FEATURES.register("crystal_column", () -> new ProcedureFeature(CrystalColumnGenerationProcedure::execute));
	public static final Supplier<Feature<NoneFeatureConfiguration>> CALCITE_MOUNTAIN =
		FEATURES.register("calcite_mountain", () -> new ProcedureFeature(CalciteMountainGenerationProcedure::execute));

	// Vanilla cave carvers don't count Crystalline Calcite as carvable, so where they cut under a floor the biome's surface
	// rule had already crusted, the slab is left floating. This runs once per chunk after all carving and clears those.
	public static final Supplier<Feature<NoneFeatureConfiguration>> CALCITE_CLEANUP =
		FEATURES.register("calcite_cleanup", CalciteCleanup::new);

	public interface Procedure {
		void execute(net.minecraft.world.level.LevelAccessor world, double x, double y, double z);
	}

	private static class ProcedureFeature extends Feature<NoneFeatureConfiguration> {
		private final Procedure procedure;

		ProcedureFeature(Procedure procedure) {
			super(NoneFeatureConfiguration.CODEC);
			this.procedure = procedure;
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			BlockPos origin = context.origin();
			procedure.execute(context.level(), origin.getX(), origin.getY(), origin.getZ());
			return true;
		}
	}

	private static class CalciteCleanup extends Feature<NoneFeatureConfiguration> {
		CalciteCleanup() {
			super(NoneFeatureConfiguration.CODEC);
		}

		@Override
		public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
			WorldGenLevel level = context.level();
			int minX = context.origin().getX() & ~15, minZ = context.origin().getZ() & ~15;
			BlockState crystalline = StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get().defaultBlockState();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			boolean changed = false;
			for (int x = minX; x < minX + 16; x++)
				for (int z = minZ; z < minZ + 16; z++)
					for (int y = level.getMinY() + 1; y < level.getMaxY(); y++) {
						if (level.getBlockState(pos.set(x, y, z)).is(crystalline.getBlock()) && level.isEmptyBlock(pos.set(x, y - 1, z))) {
							level.setBlock(pos.set(x, y, z), Blocks.AIR.defaultBlockState(), 2);
							changed = true;
						}
					}
			return changed;
		}
	}

	public static void init(IEventBus eventBus) {
		FEATURES.register(eventBus);
	}
}

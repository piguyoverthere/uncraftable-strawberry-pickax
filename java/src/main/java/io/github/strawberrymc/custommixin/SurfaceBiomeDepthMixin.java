package io.github.strawberrymc.custommixin;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.strawberrymc.init.StrawberrymcModBiomes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

// MCreator adds custom surface biomes on top of vanilla's, but vanilla already covers every climate value,
// so the custom biome only ties with the vanilla one and wins at random (often underground or in the air).
// This cuts each listed biome's climate box out of every other surface biome, so inside its climate ranges
// it is the only choice, exactly like a vanilla biome. Cave biomes (other depths) are left alone.
// Lives outside MCreator's generated files so it isn't overwritten.
@Mixin(StrawberrymcModBiomes.class)
public class SurfaceBiomeDepthMixin {
	private static final List<ResourceKey<Biome>> SURFACE_BIOMES = List.of(
		ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("strawberrymc", "alpine_meadow"))
	);

	@Inject(method = "modifyOverworldParameterPoints", at = @At("RETURN"), cancellable = true)
	private static <T> void strawberrymc$claimClimate(Climate.ParameterList<T> originalList, Function<ResourceKey<Biome>, T> lookup,
			CallbackInfoReturnable<Climate.ParameterList<T>> cir) {
		List<Pair<Climate.ParameterPoint, T>> parameters = new ArrayList<>(cir.getReturnValue().values());

		for (ResourceKey<Biome> key : SURFACE_BIOMES) {
			T biome = lookup.apply(key);
			List<Climate.Parameter[]> claimed = parameters.stream().filter(pair -> pair.getSecond().equals(biome) && isSurface(pair.getFirst()))
					.map(pair -> climateOf(pair.getFirst())).toList();

			for (Climate.Parameter[] box : claimed) {
				List<Pair<Climate.ParameterPoint, T>> carved = new ArrayList<>();
				for (Pair<Climate.ParameterPoint, T> pair : parameters) {
					if (pair.getSecond().equals(biome) || !isSurface(pair.getFirst())) {
						carved.add(pair);
						continue;
					}
					for (Climate.Parameter[] piece : subtract(climateOf(pair.getFirst()), box))
						carved.add(new Pair<>(withClimate(pair.getFirst(), piece), pair.getSecond()));
				}
				parameters = carved;
			}
		}
		cir.setReturnValue(new Climate.ParameterList<>(parameters));
	}

	// Surface biomes (vanilla and MCreator) use the depth points 0 and 1; cave biomes use other depths
	private static boolean isSurface(Climate.ParameterPoint point) {
		Climate.Parameter depth = point.depth();
		return depth.min() == depth.max() && (depth.min() == Climate.quantizeCoord(0.0f) || depth.min() == Climate.quantizeCoord(1.0f));
	}

	private static Climate.Parameter[] climateOf(Climate.ParameterPoint point) {
		return new Climate.Parameter[]{point.temperature(), point.humidity(), point.continentalness(), point.erosion(), point.weirdness()};
	}

	private static Climate.ParameterPoint withClimate(Climate.ParameterPoint point, Climate.Parameter[] climate) {
		return new Climate.ParameterPoint(climate[0], climate[1], climate[2], climate[3], point.depth(), climate[4], point.offset());
	}

	// Splits `from` into the boxes that lie outside `cut` (empty if `from` is entirely inside it)
	private static List<Climate.Parameter[]> subtract(Climate.Parameter[] from, Climate.Parameter[] cut) {
		for (int i = 0; i < from.length; i++) {
			if (from[i].max() <= cut[i].min() || from[i].min() >= cut[i].max())
				return Collections.singletonList(from);
		}

		List<Climate.Parameter[]> pieces = new ArrayList<>();
		Climate.Parameter[] rest = from.clone();
		for (int i = 0; i < from.length; i++) {
			if (rest[i].min() < cut[i].min()) {
				Climate.Parameter[] below = rest.clone();
				below[i] = new Climate.Parameter(rest[i].min(), cut[i].min());
				pieces.add(below);
			}
			if (rest[i].max() > cut[i].max()) {
				Climate.Parameter[] above = rest.clone();
				above[i] = new Climate.Parameter(cut[i].max(), rest[i].max());
				pieces.add(above);
			}
			rest[i] = new Climate.Parameter(Math.max(rest[i].min(), cut[i].min()), Math.min(rest[i].max(), cut[i].max()));
		}
		return pieces;
	}
}

package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.mojang.datafixers.util.Pair;
import com.y271727uy.farmerstales.FTMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Assign registered forest biomes continuous climate windows before chunks generate. */
@Mod.EventBusSubscriber(modid = FTMod.MODID)
public final class ForestBiomes {
    public static final ResourceKey<Biome> PALE_GARDEN = key("pale_garden");
    public static final ResourceKey<Biome> DAPPLED_FOREST = key("dappled_forest");

    private ForestBiomes() {}

    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(FTMod.MODID, name));
    }

    public interface ClimateAccess {
        void farmerstales$installForests(Registry<Biome> biomes);
        Climate.ParameterList<Holder<Biome>> farmerstales$forestParameters();
    }

    @SubscribeEvent
    public static void beforeServerStart(ServerAboutToStartEvent event) {
        var access = event.getServer().registryAccess();
        var overworld = access.registryOrThrow(Registries.LEVEL_STEM).get(LevelStem.OVERWORLD);
        if (overworld != null && overworld.generator().getBiomeSource() instanceof MultiNoiseBiomeSource source) {
            ((ClimateAccess) source).farmerstales$installForests(access.registryOrThrow(Registries.BIOME));
        }
    }

    public static Climate.ParameterList<Holder<Biome>> addForests(
            Climate.ParameterList<Holder<Biome>> original, Registry<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> result = new ArrayList<>(original.values());
        for (var region : List.of(
                new ForestRegion(window(-0.05F, 0.2F, 0.3F, 1.0F), biomes.getHolderOrThrow(PALE_GARDEN)),
                new ForestRegion(window(-0.45F, -0.15F, -0.1F, 0.3F), biomes.getHolderOrThrow(DAPPLED_FOREST)))) {
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> next = new ArrayList<>();
            for (var entry : result) {
                if (!isForestLand(entry.getSecond())) {
                    next.add(entry);
                    continue;
                }
                var overlap = intersection(entry.getFirst(), region.climate());
                if (overlap == null) {
                    next.add(entry);
                    continue;
                }
                // Partition, rather than overlay equal-distance entries. Every
                // original climate point still has exactly one interior owner.
                subtract(entry.getFirst(), overlap, entry.getSecond(), next);
                next.add(Pair.of(overlap, region.biome()));
            }
            result = next;
        }
        return new Climate.ParameterList<>(result);
    }

    private record ForestRegion(Climate.ParameterPoint climate, Holder<Biome> biome) {}

    private static Climate.ParameterPoint window(float cold, float warm, float dry, float wet) {
        return new Climate.ParameterPoint(Climate.Parameter.span(cold, warm), Climate.Parameter.span(dry, wet),
                Climate.Parameter.span(0.03F, 1.0F), Climate.Parameter.span(-0.2225F, 0.55F),
                Climate.Parameter.span(-1.0F, 1.0F), Climate.Parameter.span(-1.0F, 1.0F), 0L);
    }

    public static boolean isForestLand(Holder<Biome> biome) {
        // Preserve oceans, rivers, beaches, caves and specialized terrain. The
        // windows span several land biomes instead of borrowing strips of taiga.
        return biome.is(Biomes.PLAINS) || biome.is(Biomes.SUNFLOWER_PLAINS) || biome.is(Biomes.FOREST)
                || biome.is(Biomes.FLOWER_FOREST) || biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.DARK_FOREST)
                || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST) || biome.is(Biomes.TAIGA)
                || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                || biome.is(Biomes.MEADOW);
    }

    private static Climate.Parameter[] axes(Climate.ParameterPoint point) {
        return new Climate.Parameter[]{point.temperature(), point.humidity(), point.continentalness(),
                point.erosion(), point.depth(), point.weirdness()};
    }

    private static Climate.ParameterPoint point(Climate.Parameter[] axes, long offset) {
        return new Climate.ParameterPoint(axes[0], axes[1], axes[2], axes[3], axes[4], axes[5], offset);
    }

    private static Climate.ParameterPoint intersection(Climate.ParameterPoint source, Climate.ParameterPoint window) {
        var axes = axes(source);
        var limits = axes(window);
        for (int axis = 0; axis < axes.length; axis++) {
            long low = Math.max(axes[axis].min(), limits[axis].min());
            long high = Math.min(axes[axis].max(), limits[axis].max());
            if (low > high) return null;
            axes[axis] = new Climate.Parameter(low, high);
        }
        return point(axes, source.offset());
    }

    private static void subtract(Climate.ParameterPoint source, Climate.ParameterPoint cut, Holder<Biome> biome,
            List<Pair<Climate.ParameterPoint, Holder<Biome>>> output) {
        var remaining = axes(source);
        var inside = axes(cut);
        for (int axis = 0; axis < remaining.length; axis++) {
            var range = remaining[axis];
            if (range.min() < inside[axis].min()) {
                var lower = remaining.clone();
                lower[axis] = new Climate.Parameter(range.min(), inside[axis].min() - 1);
                output.add(Pair.of(point(lower, source.offset()), biome));
            }
            if (range.max() > inside[axis].max()) {
                var upper = remaining.clone();
                upper[axis] = new Climate.Parameter(inside[axis].max() + 1, range.max());
                output.add(Pair.of(point(upper, source.offset()), biome));
            }
            remaining[axis] = inside[axis];
        }
    }
}

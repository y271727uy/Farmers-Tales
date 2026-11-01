package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import java.util.*;

/** Client-only color data, reloaded with resource packs just like vanilla foliage. */
public final class DryFoliageColors extends SimplePreparableReloadListener<DryFoliageColors.Data> {
    public static final int DEFAULT = 0x5C3C32;
    private static volatile Data colors = new Data(new int[0], Map.of());
    public record Data(int[] pixels, Map<String,Integer> overrides) {}
    public static final ColorResolver RESOLVER = (biome,x,z) -> {
        var level = Minecraft.getInstance().level;
        var key = level == null ? null : level.registryAccess().registryOrThrow(Registries.BIOME).getKey(biome);
        var override = key == null ? null : colors.overrides.get(key.toString());
        return override != null ? override : sample(colors.pixels, biome.getBaseTemperature(), biome.getModifiedClimateSettings().downfall());
    };
    /** Matches the official colormap temperature/downfall lookup, including clamping. */
    public static int sample(int[] pixels, double temperature, double downfall) {
        double heat=Mth.clamp(temperature,0,1), humidity=Mth.clamp(downfall,0,1)*heat;
        int index=((int)((1-humidity)*255)<<8)|(int)((1-heat)*255);
        return index < pixels.length ? pixels[index] & 0xFFFFFF : DEFAULT;
    }
    @Override protected Data prepare(ResourceManager manager, ProfilerFiller profiler) {
        try {
            int[] pixels=LegacyStuffWrapper.getPixels(manager,ResourceLocation.fromNamespaceAndPath("farmerstales","textures/colormap/dry_foliage.png"));
            if(pixels.length!=65536) throw new IllegalStateException("Dry foliage colormap must be 256 x 256");
            var overrides=new HashMap<String,Integer>();
            try(var reader=manager.openAsReader(ResourceLocation.fromNamespaceAndPath("farmerstales","dry_foliage_overrides.json"))) {
                JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry->overrides.put(entry.getKey(),entry.getValue().getAsInt()));
            }
            return new Data(pixels,Map.copyOf(overrides));
        } catch(java.io.IOException exception) { throw new IllegalStateException("Cannot load dry foliage colors",exception); }
    }
    @Override protected void apply(Data data,ResourceManager manager,ProfilerFiller profiler) { colors=data; }
}

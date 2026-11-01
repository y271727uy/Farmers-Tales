package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.environment.forest.ForestBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.stream.Collectors;

/** Runtime overlay preserves the original biome-source codec and saved preset. */
@Mixin(MultiNoiseBiomeSource.class)
public abstract class ForestClimateMixin extends BiomeSource implements ForestBiomes.ClimateAccess {
    @Unique private Climate.ParameterList<Holder<Biome>> farmerstales$forests;
    @Unique private Climate.ParameterList<Holder<Biome>> farmerstales$originalClimate;
    @Unique private Set<Holder<Biome>> farmerstales$possibleForests;

    @Shadow private Climate.ParameterList<Holder<Biome>> parameters() { throw new AssertionError(); }

    @Override
    public void farmerstales$installForests(Registry<Biome> biomes) {
        if (farmerstales$forests != null) return;
        farmerstales$originalClimate = parameters();
        farmerstales$forests = ForestBiomes.addForests(farmerstales$originalClimate, biomes);
        farmerstales$possibleForests = farmerstales$forests.values().stream()
                .map(pair -> pair.getSecond()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Climate.ParameterList<Holder<Biome>> farmerstales$forestParameters() {
        return parameters();
    }

    @Inject(method = "parameters", at = @At("HEAD"), cancellable = true)
    private void farmerstales$useForests(CallbackInfoReturnable<Climate.ParameterList<Holder<Biome>>> callback) {
        if (farmerstales$forests != null) callback.setReturnValue(farmerstales$forests);
    }

    @Inject(method = "getNoiseBiome(Lnet/minecraft/world/level/biome/Climate$TargetPoint;)Lnet/minecraft/core/Holder;",
            at = @At("RETURN"), cancellable = true)
    private void farmerstales$preserveTerrain(Climate.TargetPoint target, CallbackInfoReturnable<Holder<Biome>> callback) {
        if (farmerstales$originalClimate == null) return;
        // Native climate cells share faces and may tie outside their bounds.
        // Partitioning can change R-tree tie order: keep the original winner for
        // rivers, oceans, caves and special terrain rather than erasing a river edge.
        var original = farmerstales$originalClimate.findValue(target);
        if (!ForestBiomes.isForestLand(original)) callback.setReturnValue(original);
    }

    @Override
    public Set<Holder<Biome>> possibleBiomes() {
        return farmerstales$possibleForests != null ? farmerstales$possibleForests : super.possibleBiomes();
    }
}

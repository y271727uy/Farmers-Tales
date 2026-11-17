package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.weather.area_weather.AreaWeatherEffects;
import forestry.api.climate.IClimateProvider;
import forestry.api.genetics.IGenome;
import forestry.api.genetics.IMutation;
import forestry.core.genetics.mutations.Mutation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional Forestry integration, gated by FarmersTalesMixinPlugin. Zero chances remain zero. */
@Mixin(value = Mutation.class, remap = false)
public abstract class AreaWeatherMutationMixin {
    @Inject(method = "getChance(Lforestry/api/genetics/IMutation;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lforestry/api/genetics/IGenome;Lforestry/api/genetics/IGenome;Lforestry/api/climate/IClimateProvider;)F",
            at = @At("RETURN"), cancellable = true)
    private static void farmerstales$breeze(IMutation<?> mutation, Level level, BlockPos pos,
            IGenome first, IGenome second, IClimateProvider climate, CallbackInfoReturnable<Float> callback) {
        callback.setReturnValue(AreaWeatherEffects.mutationChance(level, callback.getReturnValue()));
    }
}

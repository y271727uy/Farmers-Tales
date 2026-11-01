package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.HeavyRainClientVisuals;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererHeavyRainMixin {
    @Unique
    private float farmerstales$rainDensity = 1.0F;

    // Resolve the biome once per render/tick, not once for every rain-sheet vertex.
    @Inject(method = {"renderSnowAndRain", "tickRain"}, at = @At("HEAD"))
    private void farmerstales$prepareRainDensity(CallbackInfo ci) {
        farmerstales$rainDensity = HeavyRainClientVisuals.densityMultiplier();
    }

    // Repeat vanilla rain streaks a little more densely without adding geometry or changing snow.
    @ModifyArg(method = "renderSnowAndRain", index = 0,
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;uv(FF)Lcom/mojang/blaze3d/vertex/VertexConsumer;"),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;RAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;SNOW_LOCATION:Lnet/minecraft/resources/ResourceLocation;")))
    private float farmerstales$rainTextureDensity(float u) {
        return u * farmerstales$rainDensity;
    }

    // Vanilla still respects particle settings and checks precipitation/roof height itself.
    @ModifyVariable(method = "tickRain", at = @At("STORE"), ordinal = 0)
    private float farmerstales$rainSplashDensity(float rain) {
        return rain * (float) Math.sqrt(farmerstales$rainDensity);
    }

    @ModifyArg(method = "tickRain", index = 3,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"))
    private float farmerstales$rainVolume(float volume) {
        return volume * farmerstales$rainDensity;
    }
}

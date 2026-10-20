package com.y271727uy.farmerstales.mixin.alexmob;

import java.util.concurrent.ThreadLocalRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reduces Alex's Mobs crop-raiding attempts to a 25 percent success rate. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.CrowAICircleCrops")
public abstract class AlexsMobsCrowCropDamageMixin {
    @Inject(method = "destroyCrop", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$rollCropDamageChance(CallbackInfo ci) {
        if (ThreadLocalRandom.current().nextFloat() >= 0.25F) {
            ci.cancel();
        }
    }
}

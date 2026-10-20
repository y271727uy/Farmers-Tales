package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonLivestock;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A crimson night holds the egg timer so the hen does not lay until morning. */
@Mixin(Chicken.class)
public abstract class ChickenCrimsonMoonMixin {
    @Shadow
    public int eggTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void farmerstales$holdEggOnCrimsonMoon(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (CrimsonMoonLivestock.shouldHoldEgg(chicken, eggTime)) {
            eggTime = 2;
        }
    }
}

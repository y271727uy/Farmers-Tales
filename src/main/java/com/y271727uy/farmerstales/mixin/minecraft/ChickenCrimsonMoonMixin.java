package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalEmotion;
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonLivestock;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A crimson night or a terrified hen holds the egg timer so no egg is laid. */
@Mixin(Chicken.class)
public abstract class ChickenCrimsonMoonMixin {
    @Shadow
    public int eggTime;

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void farmerstales$holdEggOnCrimsonMoon(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (CrimsonMoonLivestock.shouldHoldEgg(chicken, eggTime)) {
            eggTime = 2;
        } else if (AnimalEmotion.shouldHoldEgg(chicken, eggTime)) {
            eggTime = AnimalEmotion.eggRecheckDelay(chicken);
        }
    }
}

package com.y271727uy.farmerstales.mixin.farmandcharm;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalMetabolism;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limits trough seeking to hungry, domesticated adult animals. */
@Pseudo
@Mixin(targets = "net.satisfy.farm_and_charm.core.entity.ai.ApproachFeedingTroughGoal")
public abstract class ApproachFeedingTroughGoalMixin {
    @Shadow @Final protected Animal animal;

    @Inject(method = {"canUse", "m_8036_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$requireHungryDomesticatedAnimal(CallbackInfoReturnable<Boolean> cir) {
        if (!canUseTrough(animal)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = {"canContinueToUse", "m_8045_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$stopWhenNoLongerHungry(CallbackInfoReturnable<Boolean> cir) {
        if (!canUseTrough(animal)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean canUseTrough(Animal animal) {
        return AnimalMetabolism.INSTANCE.isManaged(animal)
            && !AnimalMetabolism.INSTANCE.isFullyFed(animal);
    }
}

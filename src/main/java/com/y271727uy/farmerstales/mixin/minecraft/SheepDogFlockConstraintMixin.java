package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.SheepDogFlockConstraintGoal;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Holds only sheep enrolled in an active dog task, leaving ordinary sheep entirely vanilla. */
@Mixin(Sheep.class)
public abstract class SheepDogFlockConstraintMixin {
    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void farmerstales$addDogFlockConstraint(CallbackInfo ci) {
        Sheep sheep = (Sheep) (Object) this;
        sheep.goalSelector.addGoal(2, new SheepDogFlockConstraintGoal(sheep));
    }
}

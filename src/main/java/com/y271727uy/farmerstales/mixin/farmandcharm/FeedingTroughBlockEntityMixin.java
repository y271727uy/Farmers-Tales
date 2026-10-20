package com.y271727uy.farmerstales.mixin.farmandcharm;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalMetabolism;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the normal domesticated-animal nutrition when a trough serving is consumed. */
@Pseudo
@Mixin(targets = "net.satisfy.farm_and_charm.core.block.entity.FeedingTroughBlockEntity")
public abstract class FeedingTroughBlockEntityMixin {
    private static final int TROUGH_NUTRITION = 5;

    @Inject(method = "onAnimalFed", at = @At("TAIL"), require = 0)
    private void farmerstales$feedDomesticatedAnimal(Animal animal, CallbackInfo ci) {
        AnimalMetabolism.INSTANCE.feed(animal, TROUGH_NUTRITION);
    }
}

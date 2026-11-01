package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.variant.AgeLockHolder;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Animal.class)
public abstract class AnimalAgeLockMixin {
    // Gate only Animal's growth feeding branch, preserving healing and taming in subclasses.
    @Redirect(method="mobInteract",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/animal/Animal;isBaby()Z"))
    private boolean canGrow(Animal animal){return animal.isBaby()&&!((AgeLockHolder)animal).farmerstales$isAgeLocked();}
}

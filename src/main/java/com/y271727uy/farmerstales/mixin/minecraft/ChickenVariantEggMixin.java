package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.animal_variants.VariantEggs;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Chicken.class)
public abstract class ChickenVariantEggMixin {
    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Chicken;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"), index = 0)
    private ItemLike farmerstales$variantEgg(ItemLike original) {
        return VariantEggs.laidBy((Chicken) (Object) this, original);
    }
}

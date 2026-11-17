package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.backport.Backport;
import com.y271727uy.farmerstales.backport.BackportFeature;

import com.y271727uy.farmerstales.gameplay.animal_variants.VariantEggs;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Backport(BackportFeature.FARM_ANIMAL_VARIANTS)
@Mixin(Chicken.class)
public abstract class ChickenVariantEggMixin {
    // Farm & Charm's redirect adds a Chicken parameter before ItemLike.
    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Chicken;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemLike farmerstales$variantEgg(ItemLike original) {
        return VariantEggs.laidBy((Chicken) (Object) this, original);
    }
}

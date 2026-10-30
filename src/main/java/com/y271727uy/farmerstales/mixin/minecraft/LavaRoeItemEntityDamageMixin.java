package com.y271727uy.farmerstales.mixin.minecraft;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntity.class)
public abstract class LavaRoeItemEntityDamageMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void farmerstales$protectLavaRoeFromFire(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if ((source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypes.LAVA))
                && itemEntity.getItem().getItem().isFireResistant()) {
            cir.setReturnValue(true);
        }
    }
}

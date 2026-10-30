package com.y271727uy.farmerstales.mixin.minecraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class LavaRoeEntityLavaProtectionMixin {
    @Inject(method = "lavaHurt()V", at = @At("HEAD"), cancellable = true)
    private void farmerstales$skipLavaDamageForLavaRoe(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof ItemEntity itemEntity && itemEntity.getItem().getItem().isFireResistant()) {
            ci.cancel();
        }
    }
}

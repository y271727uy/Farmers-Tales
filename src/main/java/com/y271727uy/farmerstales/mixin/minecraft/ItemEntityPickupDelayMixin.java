package com.y271727uy.farmerstales.mixin.minecraft;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ItemEntity.pickupDelay has a setter but no getter. */
@Mixin(ItemEntity.class)
public interface ItemEntityPickupDelayMixin {
    @Accessor("pickupDelay")
    int getPickupDelay();
}

package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.food_expiry.FoodExpiryCapabilitySync;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuFoodExpiryMixin {
    @Redirect(method = "synchronizeSlotToRemote", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;equals(Lnet/minecraft/world/item/ItemStack;Z)Z", remap = false))
    private boolean farmerstales$syncFoodExpiry(ItemStack stack, ItemStack remoteStack, boolean limitTags) {
        return !FoodExpiryCapabilitySync.hasExpiryData(stack) && stack.equals(remoteStack, limitTags);
    }
}

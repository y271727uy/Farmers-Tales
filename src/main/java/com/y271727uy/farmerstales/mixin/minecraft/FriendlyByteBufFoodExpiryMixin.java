package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.food_expiry.FoodExpiryCapabilitySync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufFoodExpiryMixin {
    @Redirect(method = "writeItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeNbt(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/network/FriendlyByteBuf;"))
    private FriendlyByteBuf farmerstales$writeFoodExpiry(FriendlyByteBuf buffer, CompoundTag tag, ItemStack stack) {
        return buffer.writeNbt(FoodExpiryCapabilitySync.writeToNetwork(stack, tag));
    }

    @Redirect(method = "readItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;readShareTag(Lnet/minecraft/nbt/CompoundTag;)V", remap = false))
    private void farmerstales$readFoodExpiry(ItemStack stack, CompoundTag tag) {
        FoodExpiryCapabilitySync.readFromNetwork(stack, tag);
    }
}

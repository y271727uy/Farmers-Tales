package com.y271727uy.farmerstales.gameplay.food_expiry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Keeps expiry capability data in item packets without leaving it in stack NBT. */
public final class FoodExpiryCapabilitySync {
    private static final String NETWORK_KEY = "farmerstales:food_expiry";
    private static final String EMPTY_TAG_KEY = "farmerstales:empty_tag";

    private FoodExpiryCapabilitySync() {}

    @Nullable
    public static CompoundTag writeToNetwork(ItemStack stack, @Nullable CompoundTag originalTag) {
        final FoodExpiryCapabilities.Data data = FoodExpiryCapabilities.get(stack);
        if (data == null || !hasExpiryData(data)) {
            return originalTag;
        }
        final CompoundTag writable = originalTag == null ? new CompoundTag() : originalTag.copy();
        if (originalTag != null && originalTag.isEmpty()) {
            writable.putBoolean(EMPTY_TAG_KEY, true);
        }
        writable.put(NETWORK_KEY, data.serializeNBT());
        return writable;
    }

    public static void readFromNetwork(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null) {
            stack.readShareTag(null);
            return;
        }
        final CompoundTag foodTag = tag.contains(NETWORK_KEY, Tag.TAG_COMPOUND) ? tag.getCompound(NETWORK_KEY) : null;
        if (foodTag != null) {
            final boolean wasEmpty = tag.getBoolean(EMPTY_TAG_KEY);
            tag.remove(NETWORK_KEY);
            tag.remove(EMPTY_TAG_KEY);
            if (!wasEmpty && tag.isEmpty()) {
                tag = null;
            }
        }
        stack.readShareTag(tag);
        if (foodTag != null) {
            stack.getCapability(FoodExpiryCapabilities.CAPABILITY).ifPresent(data -> data.deserializeNBT(foodTag));
        }
    }

    public static boolean hasExpiryData(ItemStack stack) {
        final FoodExpiryCapabilities.Data data = FoodExpiryCapabilities.get(stack);
        return data != null && hasExpiryData(data);
    }

    private static boolean hasExpiryData(FoodExpiryCapabilities.Data data) {
        return data.expiry != 0L || data.rotten;
    }
}

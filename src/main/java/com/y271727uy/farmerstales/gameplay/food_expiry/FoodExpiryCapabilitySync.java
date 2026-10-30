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

    /** Mirrors TFC's synchronized network-capability serialization. */
    @Nullable
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static CompoundTag writeToNetwork(ItemStack stack, @Nullable CompoundTag originalTag) {
        synchronized (stack) {
            if (!hasSyncableCapability(stack)) {
                return originalTag;
            }
            final CompoundTag writable = originalTag == null ? new CompoundTag() : originalTag.copy();
            if (originalTag != null && originalTag.isEmpty()) {
                writable.putBoolean(EMPTY_TAG_KEY, true);
            }
            stack.getCapability(FoodExpiryCapabilities.NETWORK_CAPABILITY)
                    .ifPresent(data -> writable.put(NETWORK_KEY, data.serializeNBT()));
            return writable;
        }
    }

    public static void readFromNetwork(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag == null) {
            return;
        }
        final CompoundTag foodTag = readTagFromNetwork(tag);
        if (foodTag != null) {
            final boolean wasEmpty = tag.getBoolean(EMPTY_TAG_KEY);
            tag.remove(EMPTY_TAG_KEY);
            if (!wasEmpty && tag.isEmpty()) {
                tag = null;
            }
        }
        // Set the ordinary stack tag before resolving the capability, matching TFC's
        // ordering for providers which inspect the tag during initialization.
        stack.readShareTag(tag);
        if (foodTag != null) {
            stack.getCapability(FoodExpiryCapabilities.NETWORK_CAPABILITY)
                    .ifPresent(data -> data.deserializeNBT(foodTag));
        }
    }

    /** Presence, rather than current freshness, controls packet synchronization. */
    public static boolean hasSyncableCapability(ItemStack stack) {
        return stack.getCapability(FoodExpiryCapabilities.NETWORK_CAPABILITY).isPresent();
    }

    public static boolean hasExpiryData(ItemStack stack) {
        final FoodExpiryCapabilities.Data data = FoodExpiryCapabilities.get(stack);
        return data != null && (data.expiry != 0L || data.rotten);
    }

    @Nullable
    private static CompoundTag readTagFromNetwork(CompoundTag tag) {
        if (!tag.contains(NETWORK_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        final CompoundTag foodTag = tag.getCompound(NETWORK_KEY);
        tag.remove(NETWORK_KEY);
        return foodTag;
    }
}

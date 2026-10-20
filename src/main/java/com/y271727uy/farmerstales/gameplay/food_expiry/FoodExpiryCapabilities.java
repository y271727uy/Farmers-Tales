package com.y271727uy.farmerstales.gameplay.food_expiry;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-stack expiry data deliberately lives outside {@link ItemStack#getTag()}.
 * Several item renderers treat arbitrary stack NBT as a model variant, which
 * made otherwise ordinary food invisible in hand.
 */
public final class FoodExpiryCapabilities {
    public static final Capability<Data> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    private static final ResourceLocation KEY = new ResourceLocation(FTMod.MODID, "food_expiry");

    private FoodExpiryCapabilities() {}

    @Nullable
    public static Data get(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem().getFoodProperties() == null) {
            return null;
        }
        final Data data = stack.getCapability(CAPABILITY).orElse(null);
        if (data != null) {
            migrateLegacyTag(stack, data);
        }
        return data;
    }

    private static void migrateLegacyTag(ItemStack stack, Data data) {
        final CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("FarmerstalesFoodExpiry", Tag.TAG_LONG)) {
            return;
        }
        data.expiry = tag.getLong("FarmerstalesFoodExpiry");
        data.created = tag.getLong("FarmerstalesFoodCreated");
        data.duration = tag.getLong("FarmerstalesFoodDuration");
        data.rotten = tag.getBoolean("FarmerstalesFoodRotten");
        data.lastDecayUpdate = tag.getLong("FarmerstalesFoodLastDecayUpdate");
        tag.remove("FarmerstalesFoodExpiry");
        tag.remove("FarmerstalesFoodCreated");
        tag.remove("FarmerstalesFoodDuration");
        tag.remove("FarmerstalesFoodRotten");
        tag.remove("FarmerstalesFoodLastDecayUpdate");
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }

    @Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Events {
        @SubscribeEvent
        public static void attach(AttachCapabilitiesEvent<ItemStack> event) {
            if (event.getObject().getItem().getFoodProperties() != null) {
                event.addCapability(KEY, new Provider());
            }
        }
    }

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(Data.class);
    }

    public static final class Data implements net.minecraftforge.common.util.INBTSerializable<CompoundTag> {
        public long expiry;
        public long created;
        public long duration;
        public boolean rotten;
        public long lastDecayUpdate;

        @Override
        public CompoundTag serializeNBT() {
            final CompoundTag tag = new CompoundTag();
            tag.putLong("expiry", expiry);
            tag.putLong("created", created);
            tag.putLong("duration", duration);
            tag.putBoolean("rotten", rotten);
            tag.putLong("lastDecayUpdate", lastDecayUpdate);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            expiry = tag.getLong("expiry");
            created = tag.getLong("created");
            duration = tag.getLong("duration");
            rotten = tag.getBoolean("rotten");
            lastDecayUpdate = tag.getLong("lastDecayUpdate");
        }
    }

    private static final class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
        private final Data data = new Data();
        private final LazyOptional<Data> optional = LazyOptional.of(() -> data);

        @Override
        public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
            return capability == CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.deserializeNBT(nbt);
        }
    }
}

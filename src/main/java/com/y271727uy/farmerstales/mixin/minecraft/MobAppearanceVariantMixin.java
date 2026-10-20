package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.variant.AnimalAppearanceVariantHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobAppearanceVariantMixin implements AnimalAppearanceVariantHolder {
    @Unique
    private static final String FARMERSTALES_APPEARANCE_VARIANT_KEY = "FarmersTalesAppearanceVariant";
    @Unique
    private static final String FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED_KEY = "FarmersTalesAppearanceVariantInitialized";
    @Unique
    private static final EntityDataAccessor<String> FARMERSTALES_APPEARANCE_VARIANT =
            SynchedEntityData.defineId(Mob.class, EntityDataSerializers.STRING);
    @Unique
    private static final EntityDataAccessor<Boolean> FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED =
            SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void farmerstales$defineAppearanceVariant(CallbackInfo ci) {
        this.farmerstales$entityData().define(FARMERSTALES_APPEARANCE_VARIANT, "");
        this.farmerstales$entityData().define(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED, false);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void farmerstales$saveAppearanceVariant(CompoundTag tag, CallbackInfo ci) {
        String variant = this.farmerstales$entityData().get(FARMERSTALES_APPEARANCE_VARIANT);
        if (this.farmerstales$hasAppearanceVariant()) {
            tag.putString(FARMERSTALES_APPEARANCE_VARIANT_KEY, variant);
            tag.putBoolean(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED_KEY, true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void farmerstales$loadAppearanceVariant(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(FARMERSTALES_APPEARANCE_VARIANT_KEY, Tag.TAG_STRING)) {
            this.farmerstales$entityData().set(FARMERSTALES_APPEARANCE_VARIANT, tag.getString(FARMERSTALES_APPEARANCE_VARIANT_KEY));
            this.farmerstales$entityData().set(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED, true);
        }
        if (tag.contains(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED_KEY, Tag.TAG_BYTE)) {
            this.farmerstales$entityData().set(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED,
                    tag.getBoolean(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED_KEY));
        }
    }

    @Override
    public String farmerstales$getAppearanceVariant() {
        return this.farmerstales$entityData().get(FARMERSTALES_APPEARANCE_VARIANT);
    }

    @Override
    public boolean farmerstales$hasAppearanceVariant() {
        return this.farmerstales$entityData().get(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED);
    }

    @Override
    public void farmerstales$setAppearanceVariant(String variant) {
        this.farmerstales$entityData().set(FARMERSTALES_APPEARANCE_VARIANT, variant);
        this.farmerstales$entityData().set(FARMERSTALES_APPEARANCE_VARIANT_INITIALIZED, true);
    }

    @Unique
    private SynchedEntityData farmerstales$entityData() {
        return ((Entity) (Object) this).getEntityData();
    }
}

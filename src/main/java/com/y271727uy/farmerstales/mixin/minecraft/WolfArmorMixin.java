package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.variant.WolfArmorHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Wolf.class)
public abstract class WolfArmorMixin implements WolfArmorHolder {
    @Unique private static final EntityDataAccessor<ItemStack> FARMERSTALES_ARMOR=SynchedEntityData.defineId(Wolf.class,EntityDataSerializers.ITEM_STACK);
    @Inject(method="defineSynchedData",at=@At("TAIL")) private void armorDefined(CallbackInfo ci){((Wolf)(Object)this).getEntityData().define(FARMERSTALES_ARMOR,ItemStack.EMPTY);}
    @Override public ItemStack farmerstales$getWolfArmor(){return ((Wolf)(Object)this).getEntityData().get(FARMERSTALES_ARMOR);}
    @Override public void farmerstales$setWolfArmor(ItemStack stack){((Wolf)(Object)this).getEntityData().set(FARMERSTALES_ARMOR,stack.copy());}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void saveArmor(CompoundTag tag,CallbackInfo ci){if(!farmerstales$getWolfArmor().isEmpty())tag.put("FarmersTalesWolfArmor",farmerstales$getWolfArmor().save(new CompoundTag()));}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void loadArmor(CompoundTag tag,CallbackInfo ci){farmerstales$setWolfArmor(tag.contains("FarmersTalesWolfArmor")?ItemStack.of(tag.getCompound("FarmersTalesWolfArmor")):ItemStack.EMPTY);}
}

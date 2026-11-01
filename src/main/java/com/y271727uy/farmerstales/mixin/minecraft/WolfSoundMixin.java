package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.gameplay.environment.forest.NatureSounds;
import com.y271727uy.farmerstales.variant.WolfSoundHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Wolf.class)
public abstract class WolfSoundMixin implements WolfSoundHolder {
    @Unique private static final EntityDataAccessor<Integer> FARMERSTALES_SOUND=SynchedEntityData.defineId(Wolf.class,EntityDataSerializers.INT);
    @Unique private static final String[] FARMERSTALES_NAMES={"classic","angry","big","cute","grumpy","puglin","sad"};
    @Inject(method="defineSynchedData",at=@At("TAIL")) private void soundsDefined(CallbackInfo ci){((Wolf)(Object)this).getEntityData().define(FARMERSTALES_SOUND,0);}
    @Inject(method="<init>",at=@At("TAIL")) private void soundChosen(CallbackInfo ci){var wolf=(Wolf)(Object)this;if(!wolf.level().isClientSide)farmerstales$setSoundVariant(wolf.getRandom().nextInt(7));}
    @Override public int farmerstales$getSoundVariant(){return ((Wolf)(Object)this).getEntityData().get(FARMERSTALES_SOUND);}
    @Override public void farmerstales$setSoundVariant(int value){((Wolf)(Object)this).getEntityData().set(FARMERSTALES_SOUND,Math.max(0,Math.min(6,value)));}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void soundSaved(CompoundTag tag,CallbackInfo ci){tag.putInt("FarmersTalesWolfSound",farmerstales$getSoundVariant());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void soundLoaded(CompoundTag tag,CallbackInfo ci){if(tag.contains("FarmersTalesWolfSound"))farmerstales$setSoundVariant(tag.getInt("FarmersTalesWolfSound"));}
    @Inject(method={"getAmbientSound","getHurtSound","getDeathSound"},at=@At("RETURN"),cancellable=true)
    private void variantSound(CallbackInfoReturnable<SoundEvent> cir){
        int variant=farmerstales$getSoundVariant();var sound=cir.getReturnValue();if(variant==0||sound==null)return;
        var key=ForgeRegistries.SOUND_EVENTS.getKey(sound);
        if(key!=null&&key.getNamespace().equals("minecraft")&&key.getPath().startsWith("entity.wolf.")){
            String target="entity.wolf_"+FARMERSTALES_NAMES[variant]+"."+key.getPath().substring("entity.wolf.".length());
            if(NatureSounds.ALL.containsKey(target))cir.setReturnValue(NatureSounds.get(target));
        }
    }
}

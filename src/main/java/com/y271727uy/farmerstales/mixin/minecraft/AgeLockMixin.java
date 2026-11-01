package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.variant.AgeLockHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AgeableMob.class)
public abstract class AgeLockMixin implements AgeLockHolder {
    @Unique private static final EntityDataAccessor<Boolean> FARMERSTALES_AGE_LOCKED=SynchedEntityData.defineId(AgeableMob.class,EntityDataSerializers.BOOLEAN);
    @Unique private int farmerstales$ageLockCooldown;
    @Inject(method="defineSynchedData",at=@At("TAIL")) private void defineLock(CallbackInfo ci){((AgeableMob)(Object)this).getEntityData().define(FARMERSTALES_AGE_LOCKED,false);}
    public boolean farmerstales$isAgeLocked(){return ((AgeableMob)(Object)this).getEntityData().get(FARMERSTALES_AGE_LOCKED);}
    public void farmerstales$setAgeLocked(boolean locked){((AgeableMob)(Object)this).getEntityData().set(FARMERSTALES_AGE_LOCKED,locked);}
    public int farmerstales$getAgeLockCooldown(){return farmerstales$ageLockCooldown;}
    public void farmerstales$startAgeLockCooldown(){farmerstales$ageLockCooldown=40;}
    @Inject(method="addAdditionalSaveData",at=@At("TAIL")) private void saveLock(CompoundTag tag,CallbackInfo ci){tag.putBoolean("AgeLocked",farmerstales$isAgeLocked());}
    @Inject(method="readAdditionalSaveData",at=@At("TAIL")) private void loadLock(CompoundTag tag,CallbackInfo ci){farmerstales$setAgeLocked(tag.getBoolean("AgeLocked"));farmerstales$ageLockCooldown=0;}
    // Only suppress the natural baby-age increment; adult breeding cooldown still ticks normally.
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/AgeableMob;setAge(I)V"))
    private void ageNaturally(AgeableMob mob,int age){if(!farmerstales$isAgeLocked()||mob.getAge()>=0)mob.setAge(age);}
    @Inject(method="ageUp(IZ)V",at=@At("HEAD"),cancellable=true) private void preventFeedingGrowth(int amount,boolean forced,CallbackInfo ci){if(farmerstales$isAgeLocked()&&((AgeableMob)(Object)this).isBaby())ci.cancel();}
    @Inject(method="aiStep",at=@At("TAIL")) private void tickLockParticles(CallbackInfo ci){
        if(farmerstales$ageLockCooldown>0){
            var mob=(AgeableMob)(Object)this;
            if(farmerstales$ageLockCooldown%2==0&&mob.level() instanceof ServerLevel level)
                level.sendParticles(farmerstales$isAgeLocked()?com.y271727uy.farmerstales.gameplay.building.BuildingContent.PAUSE.get():com.y271727uy.farmerstales.gameplay.building.BuildingContent.RESET.get(),mob.getRandomX(1),mob.getY()+mob.getBbHeight()+0.2,mob.getRandomZ(1),1,0,0,0,0);
            farmerstales$ageLockCooldown--;
        }
    }
}

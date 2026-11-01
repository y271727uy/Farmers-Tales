package com.y271727uy.farmerstales.gameplay.building;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.variant.AgeLockHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=FTMod.MODID)
public final class GoldenDandelionInteractions {
    public static final TagKey<EntityType<?>> CANNOT_BE_AGE_LOCKED=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath(FTMod.MODID,"cannot_be_age_locked"));
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof AgeableMob mob)||!mob.isBaby())return;
        var lock=(AgeLockHolder)mob;var stack=event.getItemStack();
        if(stack.is(BuildingContent.GOLDEN_DANDELION.get().asItem())){
            if(mob.getType().is(CANNOT_BE_AGE_LOCKED)||lock.farmerstales$getAgeLockCooldown()>0)return;
            if(!mob.level().isClientSide){
                boolean locked=!lock.farmerstales$isAgeLocked();lock.farmerstales$setAgeLocked(locked);mob.setBaby(true);lock.farmerstales$startAgeLockCooldown();
                if(locked)mob.setPersistenceRequired();
                if(!event.getEntity().getAbilities().instabuild)stack.shrink(1);
                mob.level().playSound(null,mob.blockPosition(),(locked?BuildingContent.USE:BuildingContent.UNUSE).get(),net.minecraft.sounds.SoundSource.PLAYERS,1,1);
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(mob.level().isClientSide));event.setCanceled(true);
        }
    }
}

package com.y271727uy.farmerstales.gameplay.fauna;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.variant.WolfArmorHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=FTMod.MODID)
public final class WolfArmorEvents {
    public static final TagKey<net.minecraft.world.damagesource.DamageType> BYPASS=TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath(FTMod.MODID,"bypasses_wolf_armor"));
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof Wolf wolf)||!wolf.isTame()||!event.getEntity().getUUID().equals(wolf.getOwnerUUID())||wolf.isBaby())return;
        var holder=(WolfArmorHolder)wolf;var armor=holder.farmerstales$getWolfArmor();var stack=event.getItemStack();String sound=null;
        boolean equip=stack.is(FaunaContent.WOLF_ARMOR.get())&&armor.isEmpty();
        boolean repair=stack.is(FaunaContent.SCUTE.get())&&!armor.isEmpty()&&armor.isDamaged();
        boolean remove=stack.is(Items.SHEARS)&&!armor.isEmpty()&&(!EnchantmentHelper.hasBindingCurse(armor)||event.getEntity().isCreative());
        if(!equip&&!repair&&!remove)return;
        if(!event.getLevel().isClientSide){
            if(equip){var worn=stack.copy();worn.setCount(1);holder.farmerstales$setWolfArmor(worn);sound="equip";}
            else if(repair){var worn=armor.copy();worn.setDamageValue(Math.max(0,worn.getDamageValue()-8));holder.farmerstales$setWolfArmor(worn);sound="repair";}
            else{wolf.spawnAtLocation(armor.copy());holder.farmerstales$setWolfArmor(ItemStack.EMPTY);sound="unequip";}
            if((equip||repair)&&!event.getEntity().isCreative())stack.shrink(1);
            wolf.playSound(FaunaContent.soundEvent("item.wolf_armor."+sound),1,1);
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(event.getLevel().isClientSide));event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void damage(LivingHurtEvent event){
        if(!(event.getEntity() instanceof Wolf wolf)||wolf.level().isClientSide||event.getSource().is(BYPASS)||event.getAmount()<=0)return;
        var holder=(WolfArmorHolder)wolf;var armor=holder.farmerstales$getWolfArmor();if(!armor.is(FaunaContent.WOLF_ARMOR.get()))return;
        var worn=armor.copy();int damage=(int)Math.ceil(event.getAmount());event.setAmount(0);
        worn.hurtAndBreak(damage,wolf,entity->entity.playSound(FaunaContent.soundEvent("item.wolf_armor.break"),1,1));
        holder.farmerstales$setWolfArmor(worn);
        if(!worn.isEmpty())wolf.playSound(FaunaContent.soundEvent("item.wolf_armor.damage"),1,1);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof Wolf wolf))return;var holder=(WolfArmorHolder)wolf;var armor=holder.farmerstales$getWolfArmor();
        if(!armor.isEmpty()&&!EnchantmentHelper.hasVanishingCurse(armor)&&wolf.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(wolf.level(),wolf.getX(),wolf.getY(),wolf.getZ(),armor.copy()));
        holder.farmerstales$setWolfArmor(ItemStack.EMPTY);
    }
}

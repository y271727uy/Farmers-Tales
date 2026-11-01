package com.y271727uy.farmerstales.gameplay.environment.forest;
import net.minecraft.world.item.*;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TimberBoatItem extends Item {
   private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
   private final String wood;
   private final boolean hasChest;

   public TimberBoatItem(boolean hasChest, String wood, Item.Properties properties) {
      super(properties);
      this.hasChest = hasChest;
      this.wood = wood;
   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      HitResult hitresult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
      if (hitresult.getType() == HitResult.Type.MISS) {
         return InteractionResultHolder.pass(itemstack);
      } else {
         Vec3 vec3 = player.getViewVector(1.0F);
         double d0 = 5.0D;
         List<Entity> list = level.getEntities(player, player.getBoundingBox().expandTowards(vec3.scale(5.0D)).inflate(1.0D), ENTITY_PREDICATE);
         if (!list.isEmpty()) {
            Vec3 vec31 = player.getEyePosition();

            for(Entity entity : list) {
               AABB aabb = entity.getBoundingBox().inflate((double)entity.getPickRadius());
               if (aabb.contains(vec31)) {
                  return InteractionResultHolder.pass(itemstack);
               }
            }
         }

         if (hitresult.getType() == HitResult.Type.BLOCK) {
            Boat boat = this.getBoat(level, hitresult);
            boat.setYRot(player.getYRot());
            if (!level.noCollision(boat, boat.getBoundingBox())) {
               return InteractionResultHolder.fail(itemstack);
            } else {
               if (!level.isClientSide) {
                  level.addFreshEntity(boat);
                  level.gameEvent(player, GameEvent.ENTITY_PLACE, hitresult.getLocation());
                  if (!player.getAbilities().instabuild) {
                     itemstack.shrink(1);
                  }
               }

               player.awardStat(Stats.ITEM_USED.get(this));
               return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
            }
         } else {
            return InteractionResultHolder.pass(itemstack);
         }
      }
   }

   @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type) { return 1200; }

   private Boat getBoat(Level level, HitResult hitResult) {
      return ForestTravel.createBoat(level, wood, hasChest, hitResult.getLocation());
   }
}

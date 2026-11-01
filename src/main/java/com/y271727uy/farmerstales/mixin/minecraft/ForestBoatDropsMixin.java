package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestBoatWood;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Boat.class)
public abstract class ForestBoatDropsMixin {
    @ModifyArg(method="checkFallDamage",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/vehicle/Boat;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"),index=0)
    private ItemLike ownPlanks(ItemLike original){return original==net.minecraft.world.level.block.Blocks.OAK_PLANKS&&(Object)this instanceof ForestBoatWood boat?ForestBlocks.ALL.get(boat.wood()+"_planks").get():original;}
}

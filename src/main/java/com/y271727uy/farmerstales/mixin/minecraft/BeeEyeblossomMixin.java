package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.building.BuildingContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Bee.class)
public abstract class BeeEyeblossomMixin {
    @Inject(method="isFlowerValid",at=@At("HEAD"),cancellable=true) private void closedFlower(BlockPos pos,CallbackInfoReturnable<Boolean> cir){if(((Bee)(Object)this).level().getBlockState(pos).is(ForestBlocks.CLOSED_EYEBLOSSOM.get())||((Bee)(Object)this).level().getBlockState(pos).is(BuildingContent.GOLDEN_DANDELION.get()))cir.setReturnValue(false);}
    @Inject(method="isFood",at=@At("HEAD"),cancellable=true) private void closedFood(ItemStack item,CallbackInfoReturnable<Boolean> cir){if(item.is(ForestBlocks.CLOSED_EYEBLOSSOM.get().asItem())||item.is(BuildingContent.GOLDEN_DANDELION.get().asItem()))cir.setReturnValue(false);}
}

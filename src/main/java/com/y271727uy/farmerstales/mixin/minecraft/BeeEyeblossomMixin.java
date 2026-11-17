package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.all.PaleGardenBlocks;
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
    @Inject(method="isFlowerValid",at=@At("HEAD"),cancellable=true) private void closedFlower(BlockPos pos,CallbackInfoReturnable<Boolean> cir){var state=((Bee)(Object)this).level().getBlockState(pos);if(farmerstales$rejected(state.getBlock().asItem()))cir.setReturnValue(false);}
    @Inject(method="isFood",at=@At("HEAD"),cancellable=true) private void closedFood(ItemStack item,CallbackInfoReturnable<Boolean> cir){if(farmerstales$rejected(item.getItem()))cir.setReturnValue(false);}
    /** Closed eyeblossoms and golden dandelions are neither flowers nor food for bees. */
    @org.spongepowered.asm.mixin.Unique private static boolean farmerstales$rejected(net.minecraft.world.item.Item item){
        return item!=net.minecraft.world.item.Items.AIR&&(PaleGardenBlocks.CLOSED_EYEBLOSSOM.isPresent()&&item==PaleGardenBlocks.CLOSED_EYEBLOSSOM.get().asItem()
                ||BuildingContent.GOLDEN_DANDELION.isPresent()&&item==BuildingContent.GOLDEN_DANDELION.get().asItem());
    }
}

package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.building.BuildingContent;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.world.entity.animal.Bee$BeePollinateGoal")
public abstract class BeePollinationEyeblossomMixin {
    @Shadow @Final @Mutable private java.util.function.Predicate<BlockState> VALID_POLLINATION_BLOCKS;
    @Inject(method="<init>",at=@At("TAIL")) private void filterClosed(CallbackInfo ci){VALID_POLLINATION_BLOCKS=VALID_POLLINATION_BLOCKS.and(state->!state.is(ForestBlocks.CLOSED_EYEBLOSSOM.get())&&!state.is(BuildingContent.GOLDEN_DANDELION.get()));}
}

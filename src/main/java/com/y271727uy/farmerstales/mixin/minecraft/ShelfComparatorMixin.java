package com.y271727uy.farmerstales.mixin.minecraft;
import com.y271727uy.farmerstales.gameplay.shelf.ShelfBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ComparatorBlock.class)
public abstract class ShelfComparatorMixin {
    @Redirect(method="getInputSignal",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;getAnalogOutputSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)I"))
    private int shelfRearOutput(BlockState source,Level level,BlockPos sourcePos,Level unusedLevel,BlockPos comparatorPos,BlockState comparatorState){
        if(source.getBlock() instanceof ShelfBlock&&source.getValue(ShelfBlock.FACING)!=comparatorState.getValue(ComparatorBlock.FACING))return 0;
        return source.getAnalogOutputSignal(level,sourcePos);
    }
}

package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropDrop;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Supplier;

/** Applies the prepared gene generation to the exact seed stack being popped. */
@Mixin(Block.class)
public abstract class BlockGeneCropDropMixin {
    @ModifyVariable(
            method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static ItemStack farmerstales$applyGeneration(
            ItemStack stack,
            Level level,
            Supplier<net.minecraft.world.phys.Vec3> position
    ) {
        if (level.isClientSide) {
            return stack;
        }
        return GeneCropDrop.apply(stack);
    }

    @ModifyVariable(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static ItemStack farmerstales$applyGenerationAtBlockPos(
            ItemStack stack,
            Level level,
            BlockPos position
    ) {
        if (level.isClientSide) {
            return stack;
        }
        return GeneCropDrop.apply(stack);
    }
}

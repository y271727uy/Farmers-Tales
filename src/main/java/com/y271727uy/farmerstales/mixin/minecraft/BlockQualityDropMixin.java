package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropDrop;
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropEvents;
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor;
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestContext;
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keeps vanilla Block.playerDestroy and loot-table resolution intact while
 * allowing the string-based crop whitelist to decorate the final drop stack.
 */
@Mixin(Block.class)
public abstract class BlockQualityDropMixin {
    @Inject(
            method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private static void farmerstales$prepareGeneDrop(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockEntity blockEntity,
            net.minecraft.world.entity.Entity entity,
            ItemStack tool,
            boolean dropExperience,
            CallbackInfo callback
    ) {
        if (level instanceof ServerLevel serverLevel
                && entity instanceof Player player
                && state.getBlock() instanceof CropBlock crop) {
            final var generation = GeneCropEvents.prepare(serverLevel, pos, state);
            if (generation != null) {
                GeneCropDrop.prepare(generation);
            }
        }
    }

    @Inject(method = "playerDestroy", at = @At("HEAD"), require = 0)
    private void farmerstales$captureHarvestContext(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo callback
    ) {
        QualityHarvestContext.capture(level, player, state);
        if (level instanceof ServerLevel serverLevel
                && state.getBlock() instanceof CropBlock crop) {
            if (crop.isMaxAge(state)) {
                FarmersTalesAdvancements.INSTANCE.awardIfRaining(player, serverLevel);
            }
            final var generation = GeneCropEvents.prepare(serverLevel, pos, state);
            if (generation != null) {
                GeneCropDrop.prepare(generation);
            }
        }
    }

    @Inject(method = "playerDestroy", at = @At("RETURN"), require = 0)
    private void farmerstales$clearHarvestContext(
            Level level,
            Player player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity,
            ItemStack tool,
            CallbackInfo callback
    ) {
        QualityHarvestContext.clear();
        GeneCropDrop.clear();
    }

    @ModifyVariable(
            method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static ItemStack farmerstales$applyHarvestQuality(ItemStack stack) {
        QualityHarvestContext.Context context = QualityHarvestContext.current();
        if (context == null) {
            return GeneCropDrop.apply(stack);
        }
        ItemStack processed = QualityDropProcessor.applyPlayerDestroy(stack, context);
        return GeneCropDrop.apply(processed);
    }

    @ModifyVariable(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 2
    )
    private static ItemStack farmerstales$applyHarvestQualityAtBlockPos(ItemStack stack) {
        QualityHarvestContext.Context context = QualityHarvestContext.current();
        if (context == null) {
            return GeneCropDrop.apply(stack);
        }
        ItemStack processed = QualityDropProcessor.applyPlayerDestroy(stack, context);
        return GeneCropDrop.apply(processed);
    }

}

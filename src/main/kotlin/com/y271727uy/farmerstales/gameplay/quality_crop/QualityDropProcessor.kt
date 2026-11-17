package com.y271727uy.farmerstales.gameplay.quality_crop

import com.y271727uy.farmerstales.gameplay.quality_crop.util.BerryFruitHarvestWhitelist
import com.y271727uy.farmerstales.gameplay.quality_crop.util.MDWhiteList
import com.y271727uy.farmerstales.gameplay.quality_crop.util.NonStandardCropWhitelist
import com.y271727uy.farmerstales.gameplay.quality_crop.util.StandardCropWhitelist
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState

/**
 * Applies crop quality without deciding when or how a block is harvested.
 *
 * The whitelist objects remain the compatibility boundary: their string-based
 * block/item references are intentionally not changed by the harvest refactor.
 */
object QualityDropProcessor {
    /** Rolls a tier and writes it to [stack]. Returns whether a quality was applied. */
    @JvmStatic
    fun rollInto(stack: ItemStack, player: Player?, random: RandomSource): Boolean {
        val quality = QualityRoller.roll(random) ?: return false
        QualityNbt.write(stack, quality, player)
        return true
    }

    @JvmStatic
    fun applyStandard(stack: ItemStack, player: Player?, random: RandomSource): ItemStack {
        if (stack.isEmpty || QualityNbt.hasValidQuality(stack) || !StandardCropWhitelist.matches(stack)) {
            return stack
        }
        rollInto(stack, player, random)
        return stack
    }

    @JvmStatic
    fun applyNonStandard(stack: ItemStack, block: Block, player: Player?, random: RandomSource): ItemStack {
        if (stack.isEmpty || QualityNbt.hasValidQuality(stack) || !NonStandardCropWhitelist.matches(stack, block)) {
            return stack
        }
        rollInto(stack, player, random)
        return stack
    }
    @JvmStatic
    fun applyMapped(stack: ItemStack, block: Block, player: Player?, random: RandomSource): ItemStack {
        if (stack.isEmpty || QualityNbt.hasValidQuality(stack)) {
            return stack
        }
        if (!NonStandardCropWhitelist.matches(stack, block)
            && !BerryFruitHarvestWhitelist.matches(stack, block)
            && !MDWhiteList.matches(stack, block)
        ) {
            return stack
        }
        rollInto(stack, player, random)
        return stack
    }

    @JvmStatic
    fun applyPlayerDestroy(stack: ItemStack, context: QualityHarvestContext.Context): ItemStack {
        val state = context.state()
        if (!QualityHarvestRules.canReceiveQuality(context.player())) {
            return stack
        }
        if (context.scythePick()) {
            return applyScytheHarvest(stack, state.block, context.player(), context.level().random)
        }
        if (state.block is CropBlock) {
            val crop = state.block as CropBlock
            if (!crop.isMaxAge(state)) {
                return stack
            }
            return applyStandard(stack, context.player(), context.level().random)
        }
        return applyMapped(stack, state.block, context.player(), context.level().random)
    }

    fun applyScytheHarvest(stack: ItemStack, block: Block, player: Player?, random: RandomSource): ItemStack =
        if (StandardCropWhitelist.matches(stack)) applyStandard(stack, player, random)
        else applyMapped(stack, block, player, random)

    @JvmStatic
    fun applyManorsBounty(stack: ItemStack, player: Player?, random: RandomSource): ItemStack {
        if (stack.isEmpty || QualityNbt.hasValidQuality(stack) || !MDWhiteList.matches(stack)) {
            return stack
        }
        rollInto(stack, player, random)
        return stack
    }
}

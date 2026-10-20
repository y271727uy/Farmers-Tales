package com.y271727uy.farmerstales.all.items

import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.StandardCrop
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.Tier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CropBlock

/** A shovel-tier tool that harvests and replants mature crops in a square area. */
class ScytheItem(
    tier: Tier,
    private val harvestWidth: Int,
    properties: Item.Properties,
) : ShovelItem(tier, 1.5F, -3.0F, properties) {
    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val crop = level.getBlockState(context.clickedPos).block as? CropBlock ?: return super.useOn(context)
        if (!crop.isMaxAge(level.getBlockState(context.clickedPos))) return super.useOn(context)

        if (level is ServerLevel) {
            val harvested = harvestPositions(context.clickedPos).count { pos -> harvestCrop(level, pos, context.player) }
            context.player?.let { player ->
                if (harvested > 0) {
                    context.itemInHand.hurtAndBreak(harvested, player) { it.broadcastBreakEvent(context.hand) }
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun harvestPositions(center: BlockPos): Sequence<BlockPos> = sequence {
        // Even widths are biased one block toward negative X/Z so every material keeps its exact square size.
        val startOffset = -(harvestWidth / 2)
        val endOffset = startOffset + harvestWidth - 1
        for (x in startOffset..endOffset) {
            for (z in startOffset..endOffset) {
                yield(center.offset(x, 0, z))
            }
        }
    }

    private fun harvestCrop(level: ServerLevel, pos: BlockPos, harvester: Player?): Boolean {
        val state = level.getBlockState(pos)
        val crop = state.block as? CropBlock ?: return false
        if (!crop.isMaxAge(state)) return false

        val drops = StandardCrop.getHarvestDrops(level, pos, state, shouldApplyQuality = true, harvester = harvester)
        removeReplantSeed(drops, crop.getCloneItemStack(level, pos, state).item)
        level.setBlock(pos, crop.defaultBlockState(), Block.UPDATE_ALL)
        drops.filterNot(ItemStack::isEmpty).forEach { stack -> Block.popResource(level, pos, stack) }
        return true
    }

    private fun removeReplantSeed(drops: List<ItemStack>, seed: Item) {
        drops.firstOrNull { stack -> stack.`is`(seed) }?.shrink(1)
    }
}

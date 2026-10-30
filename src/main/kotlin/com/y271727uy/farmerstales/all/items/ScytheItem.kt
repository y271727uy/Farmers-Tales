package com.y271727uy.farmerstales.all.items

import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.BerryHarvestEvents

import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.StandardCrop
import com.y271727uy.farmerstales.gameplay.quality_crop.util.BerryFruitHarvestWhitelist
import com.y271727uy.farmerstales.gameplay.quality_crop.util.StandardCropWhitelist
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.Tier
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState

private const val MANORS_PURPLE_SUGAR_CANE_BLOCK_ID = "manors_bounty:purple_sugar_cane_block"
private val STACKED_CANE_BLOCK_IDS = setOf(
    "minecraft:sugar_cane",
    MANORS_PURPLE_SUGAR_CANE_BLOCK_ID,
)

/** A shovel-tier tool that harvests mature crops and ripe bushes in a square area. */
class ScytheItem(
    tier: Tier,
    private val harvestWidth: Int,
    properties: Item.Properties,
) : ShovelItem(tier, 1.5F, -3.0F, properties) {
    override fun useOn(context: UseOnContext): InteractionResult = harvestWithContext(context)

    fun harvestWithContext(context: UseOnContext): InteractionResult {
        val level = context.level
        if (!canStartHarvest(level, context.clickedPos)) return InteractionResult.PASS

        if (level is ServerLevel) {
            val harvested = harvestPositions(context.clickedPos).count { pos -> harvestAt(level, pos, context) }
            context.player?.let { player ->
                if (harvested > 0) {
                    context.itemInHand.hurtAndBreak(harvested, player) { it.broadcastBreakEvent(context.hand) }
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    private fun canStartHarvest(level: Level, pos: BlockPos): Boolean {
        val state = level.getBlockState(pos)
        if (isStackedCane(level, pos, state)) return true
        val crop = state.block as? CropBlock
        if (crop != null) return crop.isMaxAge(state)
        return BerryHarvestEvents.isRipe(state)
    }

    private fun harvestPositions(center: BlockPos): Sequence<BlockPos> = sequence {
        val effectiveWidth = harvestWidth.coerceAtLeast(3)
        // Even widths are biased one block toward negative X/Z so every material keeps its exact square size.
        val startOffset = -(effectiveWidth / 2)
        val endOffset = startOffset + effectiveWidth - 1
        for (x in startOffset..endOffset) {
            for (z in startOffset..endOffset) {
                yield(center.offset(x, 0, z))
            }
        }
    }

    private fun harvestAt(level: ServerLevel, pos: BlockPos, context: UseOnContext): Boolean {
        val state = level.getBlockState(pos)
        if (isStackedCane(level, pos, state)) {
            return harvestStackedCane(level, pos, state, context.player)
        }
        val crop = state.block as? CropBlock
        if (crop != null) return harvestCrop(level, pos, state, crop, context.player)
        if (!BerryHarvestEvents.isRipe(state)) return false
        return harvestBush(level, pos, context)
    }

    private fun isStackedCane(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockId = blockId(state)
        if (blockId !in STACKED_CANE_BLOCK_IDS) return false
        return level.getBlockState(pos.above()).block == state.block
            || level.getBlockState(pos.below()).block == state.block
    }

    private fun harvestStackedCane(
        level: ServerLevel,
        pos: BlockPos,
        clickedState: BlockState,
        harvester: Player?,
    ): Boolean {
        val block = clickedState.block
        var bottom = pos
        while (level.getBlockState(bottom.below()).block == block) {
            bottom = bottom.below()
        }

        val positions = ArrayList<BlockPos>()
        var current = bottom.above()
        while (level.getBlockState(current).block == block) {
            positions += current
            current = current.above()
        }
        if (positions.isEmpty()) return false

        val canReceiveQuality = QualityHarvestRules.canReceiveQuality(harvester)
        for (dropPos in positions.asReversed()) {
            val state = level.getBlockState(dropPos)
            val drops = Block.getDrops(state, level, dropPos, level.getBlockEntity(dropPos))
            level.removeBlock(dropPos, false)
            for (stack in drops) {
                if (stack.isEmpty) continue
                if (canReceiveQuality) {
                    if (blockId(state) == MANORS_PURPLE_SUGAR_CANE_BLOCK_ID) {
                        QualityDropProcessor.applyMapped(stack, block, harvester, level.random)
                    } else {
                        QualityDropProcessor.applyStandard(stack, harvester, level.random)
                    }
                }
                Block.popResource(level, dropPos, stack)
            }
        }

        var base = level.getBlockState(bottom)
        base.properties.firstOrNull { it.name == "age" }?.let { property ->
            @Suppress("UNCHECKED_CAST")
            base = base.setValue(property as net.minecraft.world.level.block.state.properties.IntegerProperty, 0)
        }
        base.properties.firstOrNull { it.name == "top" }?.let { property ->
            @Suppress("UNCHECKED_CAST")
            base = base.setValue(property as net.minecraft.world.level.block.state.properties.BooleanProperty, true)
        }
        level.setBlock(bottom, base, Block.UPDATE_ALL)
        return true
    }

    private fun blockId(state: BlockState): String? =
        net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.block)?.toString()

    private fun harvestCrop(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        crop: CropBlock,
        harvester: Player?,
    ): Boolean {
        if (!crop.isMaxAge(state)) return false

        val generation = com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropEvents.prepare(level, pos)
        if (generation != null) {
            com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropDrop.prepare(generation)
        }
        val drops = StandardCrop.getHarvestDrops(level, pos, state, shouldApplyQuality = false, harvester = harvester).toMutableList()
        removeReplantSeed(drops, crop.getCloneItemStack(level, pos, state).item)
        drops.forEach { stack ->
            if (!stack.isEmpty && !QualityNbt.hasValidQuality(stack) && StandardCropWhitelist.matches(stack)) {
                QualityRoller.roll(level.random)?.let { quality -> QualityNbt.write(stack, quality, harvester) }
            }
        }
        level.setBlock(pos, crop.defaultBlockState(), Block.UPDATE_ALL)
        drops.filterNot(ItemStack::isEmpty).forEach { stack ->
            com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropDrop.apply(stack)
            Block.popResource(level, pos, stack)
        }
        com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropDrop.clear()
        return true
    }

    /**
     * Picks from the ripe block state and leaves the plant. Break drops are not used:
     * a bush's loot table drops the whole plant, and its own click plays another mod's sound.
     */
    private fun harvestBush(level: ServerLevel, pos: BlockPos, context: UseOnContext): Boolean {
        val before = level.getBlockState(pos)
        val player = context.player ?: return false
        val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        if (before.use(level, player, context.hand, hit).consumesAction()) {
            return true
        }

        val picked = BerryHarvestEvents.pickedState(before) ?: return false
        val drops = bushDrops(level, pos, before)
        if (drops.isEmpty()) return false

        level.setBlockAndUpdate(pos, picked)
        for (fruit in drops) {
            if (fruit.isEmpty) continue
            if (BerryFruitHarvestWhitelist.matches(fruit, before.block) && !QualityNbt.hasValidQuality(fruit)) {
                QualityRoller.roll(level.random)?.let { quality -> QualityNbt.write(fruit, quality, player) }
            }
            Block.popResource(level, pos, fruit)
        }
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F)
        return true
    }

    /** Uses a bush's explicit fruit method when available, then falls back to its loot table. */
    private fun bushDrops(level: ServerLevel, pos: BlockPos, state: BlockState): List<ItemStack> {
        val block = state.block
        val rare = state.properties.any { property -> property.name == "rare" && state.getValue(property) == true }
        val methodName = if (rare) "getRareDropItem" else "getDropItem"
        val method = block.javaClass.methods.firstOrNull { method -> method.name == methodName && method.parameterCount == 0 }
        (method?.invoke(block) as? ItemStack)?.let { drop ->
            if (!drop.isEmpty) {
                val fruit = drop.copy()
                fruit.count = bushFruitCount(level, block)
                return listOf(fruit)
            }
        }

        return Block.getDrops(state, level, pos, level.getBlockEntity(pos)).filterNot(ItemStack::isEmpty)
    }

    private fun bushFruitCount(level: ServerLevel, block: Block): Int {
        val minimum = intMethod(block, "getDropMin") ?: return 1
        val maximum = intMethod(block, "getDropMax") ?: return minimum
        return Mth.nextInt(level.random, minimum, maximum)
    }

    private fun intMethod(block: Block, name: String): Int? =
        block.javaClass.methods.firstOrNull { method -> method.name == name && method.parameterCount == 0 }?.invoke(block) as? Int

    private fun removeReplantSeed(drops: MutableList<ItemStack>, seed: Item) {
        drops.firstOrNull { it.`is`(seed) && !it.isEmpty }?.shrink(1)
    }

}

package com.y271727uy.farmerstales.all.items

import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestContext
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.BerryHarvestEvents
import com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest.StandardCrop
import com.y271727uy.farmerstales.gameplay.special_event.seasonal.WarmCurrent
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ShovelItem
import net.minecraft.world.item.Tier
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.common.ToolAction
import net.minecraftforge.common.ToolActions
import net.minecraftforge.registries.ForgeRegistries
import java.util.function.Consumer

private val STACKED_CROP_BLOCK_IDS = setOf(
    "minecraft:sugar_cane",
    "manors_bounty:purple_sugar_cane_block",
    "manors_bounty:corn_crop",
)

/** A shovel-tier tool that harvests mature crops and ripe bushes in a square area. */
class ScytheItem(
    tier: Tier,
    private val harvestWidth: Int,
    properties: Item.Properties,
) : ShovelItem(tier, 1.5F, -3.0F, properties) {
    override fun useOn(context: UseOnContext): InteractionResult = harvestWithContext(context)

    override fun canPerformAction(stack: ItemStack, toolAction: ToolAction): Boolean {
        if (toolAction == ToolActions.SHEARS_HARVEST) {
            val harvest = QualityHarvestContext.current()
            return harvest != null && harvest.scythePick() && ScytheHarvest.useShearsForPick(harvest.state())
        }
        return super.canPerformAction(stack, toolAction)
    }

    override fun <T : LivingEntity> damageItem(stack: ItemStack, amount: Int, entity: T, onBroken: Consumer<T>): Int {
        // Some native harvests damage the held tool. The area harvest charges for them afterward.
        val harvest = QualityHarvestContext.current()
        if (harvest != null && harvest.scythePick() && harvest.player() === entity) return 0
        return super.damageItem(stack, amount, entity, onBroken)
    }

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
        val target = harvestTarget(level, pos)
        val state = level.getBlockState(target)
        return isStackedCrop(level, target, state) || ScytheHarvest.isReady(state)
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

    private fun harvestAt(level: ServerLevel, origin: BlockPos, context: UseOnContext): Boolean {
        if (!level.hasChunkAt(origin)) return false
        val pos = harvestTarget(level, origin)
        val player = context.player
        if (player != null && !level.mayInteract(player, pos)) return false

        val state = level.getBlockState(pos)
        if (isStackedCrop(level, pos, state)) {
            return harvestStackedCrop(level, pos, state, player)
        }
        if (!ScytheHarvest.isReady(state) || !WarmCurrent.canPick(level, pos, state)) return false
        if (blockId(state) == "farmersrespite:tea_bush") return harvestRespiteTea(level, pos, state, player)
        if (player != null && pick(level, pos, state, player, context)) return true

        val age = ScytheHarvest.replantAge(state)
        if (age != null) {
            return replant(level, pos, state, age, player)
        }
        if (!ScytheHarvest.hasLegacyBushDrops(state) || player == null) return false
        return harvestBush(level, pos, state, player)
    }

    private fun harvestTarget(level: Level, pos: BlockPos): BlockPos {
        val state = level.getBlockState(pos)
        if (blockId(state) == ScytheHarvest.MAIZE
            && state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(null) == DoubleBlockHalf.UPPER
            && level.getBlockState(pos.below()).block == state.block
        ) {
            return pos.below()
        }
        val topId = ScytheHarvest.harvestableTopId(state) ?: return pos
        val upper = level.getBlockState(pos.above())
        return if (blockId(upper) == topId) pos.above() else pos
    }

    private fun isStackedCrop(level: Level, pos: BlockPos, state: BlockState): Boolean {
        val blockId = blockId(state)
        if (blockId !in STACKED_CROP_BLOCK_IDS) return false
        return level.getBlockState(pos.above()).block == state.block
            || level.getBlockState(pos.below()).block == state.block
    }

    private fun harvestStackedCrop(
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
        if (harvester != null && (!level.mayInteract(harvester, bottom) || positions.any { !level.mayInteract(harvester, it) })) {
            return false
        }

        val canReceiveQuality = QualityHarvestRules.canReceiveQuality(harvester)
        for (dropPos in positions.asReversed()) {
            val state = level.getBlockState(dropPos)
            val drops = Block.getDrops(state, level, dropPos, level.getBlockEntity(dropPos))
            level.removeBlock(dropPos, false)
            for (stack in drops) {
                if (stack.isEmpty) continue
                if (canReceiveQuality) {
                    QualityDropProcessor.applyScytheHarvest(stack, block, harvester, level.random)
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

    private fun blockId(state: BlockState): String? = ForgeRegistries.BLOCKS.getKey(state.block)?.toString()

    private fun replant(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        age: IntegerProperty,
        harvester: Player?,
    ): Boolean {
        val drops = StandardCrop.getHarvestDrops(level, pos, state, shouldApplyQuality = false, harvester = harvester)
        if (drops.none { !it.isEmpty }) return false
        val seed = state.block.getCloneItemStack(level, pos, state).item
        val replanted = state.setValue(age, state.block.defaultBlockState().getValue(age))
        if (!level.setBlockAndUpdate(pos, replanted)) return false
        if (blockId(state) == ScytheHarvest.MAIZE && level.getBlockState(pos.above()).block == state.block) {
            level.removeBlock(pos.above(), false)
        }

        // Panicles and lemongrass leaves regrow from a stalk that is already planted.
        if (!ScytheHarvest.regrowsFromStalk(state)) {
            removeReplantSeed(drops, seed)
        }
        for (stack in drops) {
            if (!stack.isEmpty) {
                if (QualityHarvestRules.canReceiveQuality(harvester)) {
                    QualityDropProcessor.applyScytheHarvest(stack, state.block, harvester, level.random)
                }
                Block.popResource(level, pos, stack)
            }
        }
        return true
    }

    private fun pick(level: ServerLevel, pos: BlockPos, state: BlockState, player: Player, context: UseOnContext): Boolean {
        val hit = if (pos == context.clickedPos) {
            BlockHitResult(context.clickLocation, context.clickedFace, pos, context.isInside)
        } else {
            BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        }
        QualityHarvestContext.captureScythe(level, player, state)
        val harvested = try {
            state.use(level, player, context.hand, hit).consumesAction() || level.getBlockState(pos) != state
        } finally {
            QualityHarvestContext.clear()
        }
        if (harvested) FarmersTalesAdvancements.awardIfRaining(player, level)
        return harvested
    }

    private fun harvestRespiteTea(level: ServerLevel, pos: BlockPos, state: BlockState, player: Player?): Boolean {
        // Farmer's Respite checks for the literal shears item when cutting back tea.
        val lower = if (state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) pos.below() else pos
        val bush = level.getBlockState(lower)
        if (bush.block != state.block) return false
        if (player != null && !level.mayInteract(player, lower)) return false
        val smallBush = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("farmersrespite", "small_tea_bush"))
            ?: return false
        val blackTeaId = ResourceLocation.fromNamespaceAndPath("farmersrespite", "black_tea_leaves")
        val tea = ForgeRegistries.ITEMS.getValue(blackTeaId) ?: return false
        if (!level.setBlockAndUpdate(lower, smallBush.defaultBlockState())) return false
        val stack = ItemStack(tea, 2 + level.random.nextInt(2))
        if (QualityHarvestRules.canReceiveQuality(player)) {
            QualityDropProcessor.applyScytheHarvest(stack, state.block, player, level.random)
        }
        Block.popResource(level, pos, stack)
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.0F)
        FarmersTalesAdvancements.awardIfRaining(player, level)
        return true
    }

    private fun harvestBush(level: ServerLevel, pos: BlockPos, before: BlockState, player: Player): Boolean {
        val picked = BerryHarvestEvents.pickedState(before) ?: return false
        val drops = bushDrops(level, pos, before)
        if (drops.isEmpty()) return false

        level.setBlockAndUpdate(pos, picked)
        for (fruit in drops) {
            if (fruit.isEmpty) continue
            if (QualityHarvestRules.canReceiveQuality(player)) {
                QualityDropProcessor.applyMapped(fruit, before.block, player, level.random)
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

    private fun removeReplantSeed(drops: List<ItemStack>, seed: Item) {
        drops.firstOrNull { it.`is`(seed) && !it.isEmpty }?.shrink(1)
    }

}

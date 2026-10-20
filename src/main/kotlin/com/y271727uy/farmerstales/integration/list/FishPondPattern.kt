package com.y271727uy.farmerstales.integration.list

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState

/**
 * 珠海渔场成型结构。相对坐标按控制器朝南时的偏移，和 list 的成型检查一致。
 * 控制器不参与检查，预览时再补上。
 */
object FishPondPattern {
    const val WATER = "water"
    const val LAVA = "lava"
    val PAGES: List<String> = listOf(WATER, LAVA)

    data class PlacedBlock(val offset: BlockPos, val state: BlockState)

    fun preview(patternName: String): List<PlacedBlock>? {
        val pool = structure(patternName) ?: return null
        val controller = BuiltInRegistries.BLOCK.get(ResourceLocation("list", "fishpond_core"))
        if (controller == Blocks.AIR) {
            return pool
        }
        return pool + PlacedBlock(
            BlockPos.ZERO,
            controller.defaultBlockState().setValue(
                net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING,
                Direction.SOUTH,
            ),
        )
    }

    fun materials(patternName: String): List<ItemStack> {
        val blocks = preview(patternName) ?: return emptyList()
        val counts = LinkedHashMap<Item, Int>()
        for (placed in blocks) {
            val item = displayItem(placed.state)
            if (item == Items.AIR) {
                continue
            }
            counts.merge(item, 1, Int::plus)
        }
        return counts.entries
            .sortedWith(compareBy<Map.Entry<Item, Int>> { it.key != controllerItem() }.thenBy { it.key.descriptionId })
            .map { ItemStack(it.key, it.value) }
    }

    private fun controllerItem(): Item? {
        val controller = BuiltInRegistries.BLOCK.get(ResourceLocation("list", "fishpond_core"))
        return if (controller == Blocks.AIR) null else controller.asItem()
    }

    private fun structure(patternName: String): List<PlacedBlock>? {
        val shell = shell()
        return when (patternName) {
            WATER -> withPool(shell, Blocks.WATER, Blocks.SAND)
            LAVA -> withPool(shell, Blocks.LAVA, Blocks.SOUL_SAND)
            else -> null
        }
    }

    private fun withPool(shell: List<PlacedBlock>, surface: Block, bed: Block): List<PlacedBlock> {
        val pattern = shell.toMutableList()
        for (x in -1..1) {
            for (z in -1 downTo -3) {
                pattern += of(x, 0, z, surface)
                pattern += of(x, -1, z, bed)
            }
        }
        return pattern
    }

    private fun shell(): List<PlacedBlock> {
        val blocks = mutableListOf<PlacedBlock>()
        addRow(blocks, -2, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS)
        addRow(blocks, -1, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS)
        blocks += of(-2, 0, 0, Blocks.STONE_BRICK_SLAB)
        blocks += of(-1, 0, 0, Blocks.SMOOTH_STONE)
        blocks += of(1, 0, 0, Blocks.SMOOTH_STONE)
        blocks += of(2, 0, 0, Blocks.STONE_BRICK_SLAB)

        for (z in -1 downTo -3) {
            addRow(blocks, -2, z, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS)
            blocks += of(-2, -1, z, Blocks.SMOOTH_STONE)
            blocks += of(2, -1, z, Blocks.SMOOTH_STONE)
            blocks += of(-2, 0, z, Blocks.SMOOTH_STONE)
            blocks += of(2, 0, z, Blocks.SMOOTH_STONE)
        }

        addRow(blocks, -2, -4, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS)
        addRow(blocks, -1, -4, Blocks.STONE_BRICKS, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS)
        blocks += of(-2, 0, -4, Blocks.STONE_BRICK_SLAB)
        blocks += of(-1, 0, -4, Blocks.SMOOTH_STONE)
        blocks += of(0, 0, -4, Blocks.SMOOTH_STONE)
        blocks += of(1, 0, -4, Blocks.SMOOTH_STONE)
        blocks += of(2, 0, -4, Blocks.STONE_BRICK_SLAB)
        return blocks
    }

    private fun addRow(blocks: MutableList<PlacedBlock>, y: Int, x0: Block, x1: Block, x2: Block, x3: Block, x4: Block) {
        addRow(blocks, y, 0, x0, x1, x2, x3, x4)
    }

    private fun addRow(blocks: MutableList<PlacedBlock>, y: Int, z: Int, x0: Block, x1: Block, x2: Block, x3: Block, x4: Block) {
        blocks += of(-2, y, z, x0)
        blocks += of(-1, y, z, x1)
        blocks += of(0, y, z, x2)
        blocks += of(1, y, z, x3)
        blocks += of(2, y, z, x4)
    }

    private fun of(x: Int, y: Int, z: Int, block: Block): PlacedBlock {
        return PlacedBlock(BlockPos(x, y, z), block.defaultBlockState())
    }

    private fun displayItem(state: BlockState): Item {
        return when {
            state.`is`(Blocks.WATER) -> Items.WATER_BUCKET
            state.`is`(Blocks.LAVA) -> Items.LAVA_BUCKET
            else -> state.block.asItem()
        }
    }
}

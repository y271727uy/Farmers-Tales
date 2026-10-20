package com.y271727uy.farmerstales.all

import com.tterrag.registrate.util.entry.BlockEntry
import com.y271727uy.farmerstales.all.blocks.AnimalHusbandryBlocks
import com.y271727uy.farmerstales.all.blocks.TreeBlocks
import com.y271727uy.farmerstales.gameplay.animal_husbandry.block.AnimalDungBlock
import com.y271727uy.farmerstales.gameplay.tree.block.TreeCompostBlock
import com.y271727uy.farmerstales.gameplay.tree.block.TreeStumpBlock

/** Aggregates block groups so the mod entry point does not know individual categories. */
object ModBlocks {
    @JvmField
    val ANIMAL_DUNG: BlockEntry<AnimalDungBlock> = AnimalHusbandryBlocks.ANIMAL_DUNG

    @JvmField
    val TREE_COMPOST: BlockEntry<TreeCompostBlock> = TreeBlocks.TREE_COMPOST

    @JvmField
    val TREE_STUMP: BlockEntry<TreeStumpBlock> = TreeBlocks.TREE_STUMP

    fun init() {
        AnimalHusbandryBlocks.init()
        TreeBlocks.init()
    }
}

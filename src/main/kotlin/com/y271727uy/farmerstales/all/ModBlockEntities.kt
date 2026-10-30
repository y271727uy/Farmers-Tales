package com.y271727uy.farmerstales.all

import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeCompostBlockEntity
import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeStumpBlockEntity
import com.y271727uy.farmerstales.registrate.ModRegistrate
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity

object ModBlockEntities {
    @JvmField
    val CROP_STAND: BlockEntityEntry<CropStandBlockEntity> = ModRegistrate.REGISTRATE
        .blockEntity("crop_stand", ::CropStandBlockEntity)
        .validBlocks(ModBlocks.CROP_STAND)
        .register()

    @JvmField
    val TREE_COMPOST: BlockEntityEntry<TreeCompostBlockEntity> = ModRegistrate.REGISTRATE
        .blockEntity("tree_compost", ::TreeCompostBlockEntity)
        .validBlocks(ModBlocks.TREE_COMPOST)
        .register()

    @JvmField
    val TREE_STUMP: BlockEntityEntry<TreeStumpBlockEntity> = ModRegistrate.REGISTRATE
        .blockEntity("tree_stump", ::TreeStumpBlockEntity)
        .validBlocks(ModBlocks.TREE_STUMP)
        .register()

    fun init() = Unit
}

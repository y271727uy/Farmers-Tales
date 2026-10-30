package com.y271727uy.farmerstales.all.items

import com.tterrag.registrate.util.entry.ItemEntry
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.item.BlockItem

object GeneCropItems {
    @JvmField
    val CROP_STAND: ItemEntry<BlockItem> = ModRegistrate.REGISTRATE
        .item("crop_stand") { properties -> BlockItem(ModBlocks.CROP_STAND.get(), properties) }
        .model { _, provider -> provider.blockItem { ModBlocks.CROP_STAND.get() } }
        .register()

    fun init() = Unit
}

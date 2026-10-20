package com.y271727uy.farmerstales.all.items

import com.tterrag.registrate.util.entry.ItemEntry
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.item.BlockItem

object AnimalHusbandryItems {
    @JvmField
    val ANIMAL_DUNG: ItemEntry<BlockItem> = ModRegistrate.REGISTRATE
        .item("animal_dung") { properties -> BlockItem(ModBlocks.ANIMAL_DUNG.get(), properties) }
        .model { _, provider -> provider.getExistingFile(provider.modLoc("block/shitblock")) }
        .register()

    fun init() = Unit
}

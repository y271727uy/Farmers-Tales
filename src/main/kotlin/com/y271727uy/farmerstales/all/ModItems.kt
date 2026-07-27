package com.y271727uy.farmerstales.all

import com.tterrag.registrate.util.entry.ItemEntry
import com.y271727uy.farmerstales.all.items.TreeItems
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item

/** Aggregates item groups so the mod entry point does not know individual categories. */
object ModItems {
    @JvmField
    val TREE_COMPOST: ItemEntry<BlockItem> = TreeItems.TREE_COMPOST

    @JvmField
    val TREE_STUMP: ItemEntry<BlockItem> = TreeItems.TREE_STUMP

    @JvmField
    val SHEPHERDS_CROOK: ItemEntry<Item> =
        ModRegistrate.REGISTRATE.item("shepherds_crook") { properties -> Item(properties) }.register()

    @JvmField
    val SPECIALIZED_CARNIVOROUS_BAIT: ItemEntry<Item> =
        ModRegistrate.REGISTRATE.item("specialized_carnivorous_bait") { properties -> Item(properties) }.register()

    @JvmField
    val SPECIALIZED_HERBIVOROUS_BAIT: ItemEntry<Item> =
        ModRegistrate.REGISTRATE.item("specialized_herbivorous_bait") { properties -> Item(properties) }.register()

    fun init() {
        TreeItems.init()
    }
}

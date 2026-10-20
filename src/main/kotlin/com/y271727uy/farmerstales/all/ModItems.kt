package com.y271727uy.farmerstales.all

import com.tterrag.registrate.util.entry.ItemEntry
import com.y271727uy.farmerstales.all.items.AnimalHusbandryItems
import com.y271727uy.farmerstales.all.items.TreeItems
import com.y271727uy.farmerstales.all.items.ToolItems
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import com.y271727uy.farmerstales.all.items.ScytheItem
import net.minecraft.resources.ResourceLocation

/** Aggregates item groups so the mod entry point does not know individual categories. */
object ModItems {
    @JvmField
    val ANIMAL_DUNG: ItemEntry<BlockItem> = AnimalHusbandryItems.ANIMAL_DUNG

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

    @JvmField
    val ANIMAL_FAT: ItemEntry<Item> =
        ModRegistrate.REGISTRATE.item("animal_fat") { properties -> Item(properties) }.register()

    /** Kept separate from the turtle scute so recipes and loot can target armadillos precisely. */
    @JvmField
    val ARMADILLO_SCUTE: ItemEntry<Item> = ModRegistrate.REGISTRATE
        .item("armadillo_scute") { properties -> Item(properties) }
        .model { context, provider -> provider.generated(context, provider.modLoc("item/armadillo_scute")) }
        .register()

    @JvmField
    val WOODEN_SCYTHE: ItemEntry<ScytheItem> = ToolItems.WOODEN_SCYTHE

    @JvmField
    val GOLDEN_SCYTHE: ItemEntry<ScytheItem> = ToolItems.GOLDEN_SCYTHE

    @JvmField
    val COPPER_SCYTHE: ItemEntry<ScytheItem> = ToolItems.COPPER_SCYTHE

    @JvmField
    val IRON_SCYTHE: ItemEntry<ScytheItem> = ToolItems.IRON_SCYTHE

    @JvmField
    val DIAMOND_SCYTHE: ItemEntry<ScytheItem> = ToolItems.DIAMOND_SCYTHE

    @JvmField
    val NETHERITE_SCYTHE: ItemEntry<ScytheItem> = ToolItems.NETHERITE_SCYTHE

    fun init() {
        AnimalHusbandryItems.init()
        TreeItems.init()
        ToolItems.init()
    }
}

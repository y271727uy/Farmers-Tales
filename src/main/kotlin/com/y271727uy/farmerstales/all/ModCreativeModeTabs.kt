package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.items.CropPollenItem
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenSample
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject

object ModCreativeModeTabs {
    private val tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FTMod.MODID)

    @JvmField
    val FARMERS_TALES: RegistryObject<CreativeModeTab> = tabs.register("farmerstales") {
        CreativeModeTab.builder()
            .icon { ItemStack(ModItems.TREE_COMPOST.get()) }
            .title(Component.translatable("itemGroup.${FTMod.MODID}"))
            .displayItems { _, output ->
                val naturalItems = naturalEnvironmentBlocks().map { it.asItem() }.toSet()
                BuiltInRegistries.ITEM.forEach { item ->
                    if (BuiltInRegistries.ITEM.getKey(item).namespace == FTMod.MODID &&
                        item !is CropPollenItem && item !in naturalItems
                    ) {
                        output.accept(item)
                    }
                }
                CropPollenDefinitions.displaySeeds().forEach { seed ->
                    output.accept(CropPollenItem.create(CropPollenSample(seed)))
                }
                naturalItems.forEach { output.accept(it) }
            }
            .build()
    }

    /**
     * Natural blocks listed last, in this order. Resolved by name while building tab contents so
     * blocks of disabled backport features are simply skipped.
     */
    private fun naturalEnvironmentBlocks(): List<Block> = (listOf(
        "pale_oak_sapling", "pale_oak_leaves", "pale_oak_log", "stripped_pale_oak_log", "pale_oak_wood",
        "stripped_pale_oak_wood", "pale_oak_planks",
        "poplar_sapling", "red_poplar_leaves", "orange_poplar_leaves", "yellow_poplar_leaves", "poplar_log",
        "stripped_poplar_log", "poplar_wood", "stripped_poplar_wood", "poplar_planks",
        "pale_moss_block", "pale_moss_carpet", "pale_hanging_moss", "closed_eyeblossom", "open_eyeblossom",
        "leaf_litter", "red_shrub", "shelf_mushroom",
        "bush", "short_dry_grass", "tall_dry_grass", "firefly_bush", "wildflowers", "cactus_flower",
    ) + listOf("pale_oak", "poplar").flatMap { tree ->
        listOf("stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate").map { "${tree}_$it" }
    }).mapNotNull { name ->
        BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, name)).orElse(null)
    }

    fun register(bus: IEventBus) {
        tabs.register(bus)
    }
}

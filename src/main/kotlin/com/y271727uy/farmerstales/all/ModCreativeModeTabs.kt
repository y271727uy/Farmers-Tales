package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.items.CropPollenItem
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenSample
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
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

    /** Resolve registry objects only while building tab contents, after block/item registration. */
    private fun naturalEnvironmentBlocks(): List<Block> = listOf(
        ForestBlocks.PALE_OAK_SAPLING.get(),
        ForestBlocks.PALE_OAK_LEAVES.get(),
        ForestBlocks.PALE_OAK_LOG.get(),
        ForestBlocks.STRIPPED_PALE_OAK_LOG.get(),
        ForestBlocks.PALE_OAK_WOOD.get(),
        ForestBlocks.STRIPPED_PALE_OAK_WOOD.get(),
        ForestBlocks.PALE_OAK_PLANKS.get(),
        ForestBlocks.POPLAR_SAPLING.get(),
        ForestBlocks.RED_POPLAR_LEAVES.get(),
        ForestBlocks.ORANGE_POPLAR_LEAVES.get(),
        ForestBlocks.YELLOW_POPLAR_LEAVES.get(),
        ForestBlocks.POPLAR_LOG.get(),
        ForestBlocks.STRIPPED_POPLAR_LOG.get(),
        ForestBlocks.POPLAR_WOOD.get(),
        ForestBlocks.STRIPPED_POPLAR_WOOD.get(),
        ForestBlocks.POPLAR_PLANKS.get(),
        ForestBlocks.PALE_MOSS_BLOCK.get(),
        ForestBlocks.PALE_MOSS_CARPET.get(),
        ForestBlocks.PALE_HANGING_MOSS.get(),
        ForestBlocks.CLOSED_EYEBLOSSOM.get(),
        ForestBlocks.OPEN_EYEBLOSSOM.get(),
        ForestBlocks.LEAF_LITTER.get(),
        ForestBlocks.RED_SHRUB.get(),
        ForestBlocks.SHELF_MUSHROOM.get(),
        NaturalEnvironmentBlocks.BUSH.get(),
        NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get(),
        NaturalEnvironmentBlocks.TALL_DRY_GRASS.get(),
        NaturalEnvironmentBlocks.FIREFLY_BUSH.get(),
        NaturalEnvironmentBlocks.WILDFLOWERS.get(),
        NaturalEnvironmentBlocks.CACTUS_FLOWER.get(),
    ) + ForestBlocks.BUILDING.values.map { it.get() }

    fun register(bus: IEventBus) {
        tabs.register(bus)
    }
}

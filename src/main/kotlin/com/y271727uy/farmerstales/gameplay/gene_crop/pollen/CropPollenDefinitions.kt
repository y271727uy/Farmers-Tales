package com.y271727uy.farmerstales.gameplay.gene_crop.pollen

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

/** Tags remain authoritative and are read live, including after datapack reloads. */
object CropPollenDefinitions {
    class FamilyBuilder {
        internal val tags = linkedSetOf<TagKey<Item>>()
        fun seeds(tag: TagKey<Item>) { tags += tag }
    }

    data class Family(val name: String, val seedTags: Set<TagKey<Item>>) {
        fun supports(seed: ItemStack): Boolean = !seed.isEmpty && seedTags.any { seed.`is`(it) }
    }

    private fun pollen(name: String, define: FamilyBuilder.() -> Unit): Family {
        val builder = FamilyBuilder().apply(define)
        require(builder.tags.isNotEmpty()) { "Pollen family $name needs a seed tag" }
        return Family(name, builder.tags.toSet())
    }

    val families: List<Family> = listOf(
        pollen("crop") {
            seeds(GeneCropGenome.SEEDS)
        },
    )

    fun supports(seed: ItemStack): Boolean = families.any { it.supports(seed) }

    /** One display variant per tagged seed, without assigning it a random genome. */
    fun displaySeeds(): List<ItemStack> = BuiltInRegistries.ITEM.map(::ItemStack).filter(::supports)
}

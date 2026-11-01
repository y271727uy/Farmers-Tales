package com.y271727uy.farmerstales.gameplay.gene_crop.pollen

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

/** Seed identity, a snapshot of the donor's genome, and an optional collection location. */
class CropPollenSample(seed: ItemStack, val source: BlockPos? = null, val dimension: ResourceLocation? = null) {
    private val parent = ItemStack(seed.item).also {
        if (!seed.isEmpty) GeneCropGenome.copyGenome(seed, it)
    }

    fun seed(): ItemStack = parent.copy()
    fun isViable(): Boolean = CropPollenDefinitions.supports(parent) && GeneCropGenome.isIdentified(parent)

    fun write(): CompoundTag = CompoundTag().also { tag ->
        tag.put("Seed", parent.save(CompoundTag()))
        if (source != null && dimension != null) {
            tag.putLong("Source", source.asLong())
            tag.putString("Dimension", dimension.toString())
        }
    }

    companion object {
        fun read(tag: CompoundTag): CropPollenSample {
            val source = if (tag.contains("Source", Tag.TAG_LONG.toInt())) BlockPos.of(tag.getLong("Source")) else null
            return CropPollenSample(ItemStack.of(tag.getCompound("Seed")), source, ResourceLocation.tryParse(tag.getString("Dimension")))
        }
    }
}

package com.y271727uy.farmerstales.gameplay.gene_crop

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.saveddata.SavedData

/**
 * A vanilla potato block cannot remember which seed planted it.
 * The planted genome stays with the world until that position is replanted or cleared.
 */
class GeneCropParents : SavedData() {
    private val parents = HashMap<String, CompoundTag>()

    fun plant(level: ServerLevel, pos: BlockPos, seed: ItemStack) {
        val genome = GeneCropGenome.stored(seed) ?: return
        if (genome.isEmpty) {
            return
        }
        parents[key(level, pos)] = genome.copy()
        setDirty()
    }

    fun read(level: ServerLevel, pos: BlockPos): ItemStack? {
        val genome = parents[key(level, pos)] ?: return null
        val seed = ItemStack(net.minecraft.world.item.Items.POTATO)
        seed.getOrCreateTag().put(GeneCropGenome.TAG, genome.copy())
        return seed
    }

    fun clear(level: ServerLevel, pos: BlockPos) {
        val removed = parents.remove(key(level, pos))
        if (removed != null) {
            setDirty()
        }
    }

    override fun save(tag: CompoundTag): CompoundTag {
        val list = ListTag()
        parents.forEach { (position, genome) ->
            list.add(CompoundTag().apply {
                putString("Pos", position)
                put(GeneCropGenome.TAG, genome.copy())
            })
        }
        tag.put("Parents", list)
        return tag
    }

    companion object {
        private const val NAME = FTMod.MODID + "_gene_crop_parents"

        @JvmStatic
        fun get(level: ServerLevel): GeneCropParents = level.dataStorage.computeIfAbsent(::load, ::GeneCropParents, NAME)

        @JvmStatic
        fun load(tag: CompoundTag): GeneCropParents = GeneCropParents().apply {
            tag.getList("Parents", Tag.TAG_COMPOUND.toInt()).forEach { element ->
                val entry = element as CompoundTag
                if (entry.contains("Pos") && entry.contains(GeneCropGenome.TAG)) {
                    parents[entry.getString("Pos")] = entry.getCompound(GeneCropGenome.TAG)
                }
            }
        }

        private fun key(level: ServerLevel, pos: BlockPos): String =
            level.dimension().location().toString() + " " + pos.asLong()
    }
}

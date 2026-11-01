package com.y271727uy.farmerstales.gameplay.gene_crop

import com.y271727uy.farmerstales.gameplay.gene_crop.block.CropStandBlock
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState

/** Breeding is opt-in: only crops inside stands can supply either parent. */
object GeneCropEvents {
    private val around = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)

    @JvmStatic
    fun prepare(level: ServerLevel, pos: BlockPos): CompoundTag? {
        val stand = standAt(level, pos) ?: return null
        return prepare(level, pos, stand.cropState)
    }

    /** Ordinary crop harvest hooks deliberately receive null, even for legacy saved parents. */
    @JvmStatic
    fun prepare(level: ServerLevel, pos: BlockPos, state: BlockState): CompoundTag? {
        val stand = standAt(level, pos) ?: return null
        if (stand.cropState != state) return null
        val mother = stand.breedingSeed() ?: return null
        if (!stand.isMature()) return GeneCropGenome.stored(mother)
        stand.pollenFather()?.let { return GeneCropGenome.inherit(mother, it, level.random) }
        val candidates = around.mapNotNull { (x, z) ->
            val neighbor = standAt(level, pos.offset(x, 0, z)) ?: return@mapNotNull null
            if (!neighbor.isMature()) return@mapNotNull null
            neighbor.breedingSeed()?.takeIf { it.`is`(mother.item) }
        }
        val father = candidates.takeIf { it.isNotEmpty() }?.let { it[level.random.nextInt(it.size)] }
        return GeneCropGenome.inherit(mother, father, level.random)
    }

    private fun standAt(level: ServerLevel, pos: BlockPos): CropStandBlockEntity? {
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).block !is CropStandBlock) return null
        return level.getBlockEntity(pos) as? CropStandBlockEntity
    }
}

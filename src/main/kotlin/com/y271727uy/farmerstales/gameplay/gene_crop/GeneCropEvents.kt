package com.y271727uy.farmerstales.gameplay.gene_crop

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.CropBlock
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Planting an identified potato keeps that genome as the crop's parent.
 * Harvest runs one Forestry generation. A neighboring identified potato is the other parent;
 * without one the crop self-pollinates.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object GeneCropEvents {
    private val around = arrayOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1, -1 to -1, -1 to 1, 1 to -1, 1 to 1)

    @JvmStatic
    @SubscribeEvent
    fun onPlant(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level as? ServerLevel ?: return
        val seed = event.itemStack
        if (!GeneCropGenome.isGeneSeed(seed)) {
            return
        }
        val target = event.pos.relative(event.face)
        if (!level.getBlockState(target).canBeReplaced()) {
            return
        }
        if (GeneCropGenome.isIdentified(seed)) {
            GeneCropParents.get(level).plant(level, target, seed)
        } else {
            GeneCropParents.get(level).clear(level, target)
        }
    }

    /** Both the scythe and a normal break build their drops through this. */
    @JvmStatic
    fun prepare(level: ServerLevel, pos: BlockPos): CompoundTag? =
        prepare(level, pos, level.getBlockState(pos))

    @JvmStatic
    fun prepare(level: ServerLevel, pos: BlockPos, state: net.minecraft.world.level.block.state.BlockState): CompoundTag? {
        val crop = state.block as? CropBlock
        if (crop == null) return null
        val mother = GeneCropParents.get(level).read(level, pos) ?: return null
        if (!crop.isMaxAge(state)) {
            return GeneCropGenome.stored(mother)
        }
        val father = neighbor(level, pos)
        return GeneCropGenome.inherit(mother, father, level.random)
    }

    private fun neighbor(level: ServerLevel, pos: BlockPos): ItemStack? {
        val parents = GeneCropParents.get(level)
        for ((x, z) in around) {
            val parent = parents.read(level, pos.offset(x, 0, z))
            if (parent != null) {
                return parent
            }
        }
        return null
    }
}

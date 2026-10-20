package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.all.ModBlocks
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

/** Places dung only in the animal's current pen cell; it never searches for a nearby position. */
object AnimalDungPlacement {
    fun tryPlaceAtFeet(animal: Animal): Boolean {
        val level = animal.level() as? ServerLevel ?: return false
        if (!animal.isAlive) {
            return false
        }

        val dungPos = animal.blockPosition()
        if (!level.getBlockState(dungPos).isAir) {
            return false
        }

        val groundPos = dungPos.below()
        val groundState = level.getBlockState(groundPos)
        if (!groundState.isFaceSturdy(level, groundPos, Direction.UP) && !groundState.`is`(Blocks.FARMLAND)) {
            return false
        }

        return level.setBlock(dungPos, ModBlocks.ANIMAL_DUNG.get().defaultBlockState(), Block.UPDATE_ALL)
    }
}

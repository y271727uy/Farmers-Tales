package com.y271727uy.farmerstales.gameplay.animal_husbandry.block

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState

/** Matches the low pile rendered by the dung block model instead of exposing a full-block outline. */
@Suppress("OVERRIDE_DEPRECATION")
class AnimalDungBlock(properties: BlockBehaviour.Properties) : Block(properties) {
    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = SHAPE

    override fun getOcclusionShape(state: BlockState, level: BlockGetter, pos: BlockPos): VoxelShape = Shapes.empty()

    override fun getVisualShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext,
    ): VoxelShape = Shapes.empty()

    companion object {
        private val SHAPE = Shapes.or(
            box(5.0, 0.0, 5.0, 11.0, 3.0, 11.0),
            box(6.0, 3.0, 6.0, 10.0, 5.0, 10.0),
            box(7.0, 5.0, 7.0, 9.0, 6.0, 9.0),
        )
    }
}

package com.y271727uy.farmerstales.gameplay.gene_crop.block

import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.server.level.ServerLevel
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.level.block.Blocks
import com.y271727uy.farmerstales.all.ModBlockEntities
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

/** Holds a virtual crop so its model and genome can coexist with the stand. */
@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class CropStandBlock(properties: BlockBehaviour.Properties) : BaseEntityBlock(properties), BonemealableBlock {
    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape = SHAPE

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        CropStandBlockEntity(ModBlockEntities.CROP_STAND.get(), pos, state)

    override fun getRenderShape(state: BlockState): RenderShape = RenderShape.MODEL

    override fun canSurvive(state: BlockState, level: LevelReader, pos: BlockPos): Boolean =
        level.getBlockState(pos.below()).block is FarmBlock

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? =
        defaultBlockState().takeIf { it.canSurvive(context.level, context.clickedPos) }

    override fun updateShape(state: BlockState, direction: Direction, neighbor: BlockState, level: net.minecraft.world.level.LevelAccessor, pos: BlockPos, neighborPos: BlockPos): BlockState =
        if (!state.canSurvive(level, pos)) Blocks.AIR.defaultBlockState()
        else super.updateShape(state, direction, neighbor, level, pos, neighborPos)

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        val stand = level.getBlockEntity(pos) as? CropStandBlockEntity ?: return InteractionResult.PASS
        val held = player.getItemInHand(hand)
        val handled = when {
            stand.isEmpty() -> stand.canPlant(held)
            held.isEmpty -> player.isShiftKeyDown || stand.isMature()
            else -> false
        }
        if (!handled) return InteractionResult.PASS
        if (!level.isClientSide) {
            when {
                stand.isEmpty() -> stand.plant(held, player.abilities.instabuild)
                player.isShiftKeyDown -> stand.uproot()
                else -> stand.harvest(player)
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun randomTick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        (level.getBlockEntity(pos) as? CropStandBlockEntity)?.growNaturally(level, random)
    }

    override fun isValidBonemealTarget(level: LevelReader, pos: BlockPos, state: BlockState, isClient: Boolean): Boolean =
        (level.getBlockEntity(pos) as? CropStandBlockEntity)?.canGrow() == true

    override fun isBonemealSuccess(level: Level, random: RandomSource, pos: BlockPos, state: BlockState): Boolean = true

    override fun performBonemeal(level: ServerLevel, random: RandomSource, pos: BlockPos, state: BlockState) {
        (level.getBlockEntity(pos) as? CropStandBlockEntity)?.grow(2 + random.nextInt(4))
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!state.`is`(newState.block) && !level.isClientSide) {
            (level.getBlockEntity(pos) as? CropStandBlockEntity)?.uproot()
        }
        super.onRemove(state, level, pos, newState, isMoving)
    }

    companion object {
        // The borrowed model extends one pixel into the soil.
        private val SHAPE = box(0.0, -0.95, 0.0, 16.0, 15.0, 16.0)
    }
}

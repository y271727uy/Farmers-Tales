package com.y271727uy.farmerstales.gameplay.creaking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public final class ResinClumpBlock extends MultifaceBlock implements SimpleWaterloggedBlock {
    private final net.minecraft.world.level.block.MultifaceSpreader spreader = new net.minecraft.world.level.block.MultifaceSpreader(this);
    @Override public net.minecraft.world.level.block.MultifaceSpreader getSpreader() { return spreader; }

    public ResinClumpBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.HONEYCOMB_BLOCK).sound(CreakingContent.blockSounds("block.resin")).noCollission().noOcclusion().instabreak());
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder); builder.add(BlockStateProperties.WATERLOGGED);
    }
    @Override public BlockState getStateForPlacement(BlockState current, BlockGetter level, BlockPos pos, Direction direction) {
        var placed = super.getStateForPlacement(current, level, pos, direction);
        return placed == null ? null : placed.setValue(BlockStateProperties.WATERLOGGED, level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER));
    }
    @Override public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        var result = super.updateShape(state, direction, neighbor, level, pos, neighborPos);
        return result.isAir() && state.getValue(BlockStateProperties.WATERLOGGED) ? Blocks.WATER.defaultBlockState() : result;
    }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return state.getFluidState().isEmpty(); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(asItem())) return InteractionResult.PASS;
        var direction = hit.getDirection().getOpposite();
        if (!hasFace(state, direction)) direction = availableFaces(state).stream().findFirst().orElse(null);
        if (direction == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            var next = state.setValue(getFaceProperty(direction), false);
            level.setBlockAndUpdate(pos, hasAnyFace(next) ? next : state.getValue(BlockStateProperties.WATERLOGGED)
                    ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
            popResource(level, pos, new ItemStack(this));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

package com.y271727uy.farmerstales.gameplay.creaking;

import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;

public final class CreakingHeartBlock extends BaseEntityBlock {
    public enum HeartState implements StringRepresentable {
        UPROOTED, DORMANT, AWAKE;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<HeartState> STATE = EnumProperty.create("creaking_heart_state", HeartState.class);
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public CreakingHeartBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).strength(10).sound(CreakingContent.blockSounds("block.creaking_heart")).lightLevel(state -> state.getValue(STATE) == HeartState.AWAKE ? 6 : 0));
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(STATE, HeartState.UPROOTED).setValue(NATURAL, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS, STATE, NATURAL); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.getValue(AXIS) == Direction.Axis.Y ? state
                    : state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CreakingHeartBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, CreakingContent.HEART_ENTITY.get(), CreakingHeartBlockEntity::tick);
    }
    public static boolean hasAlignedLogs(LevelReader level, BlockPos pos, BlockState state) {
        var axis = state.getValue(AXIS);
        for (var sign : Direction.AxisDirection.values()) {
            var log = level.getBlockState(pos.relative(Direction.get(sign, axis)));
            if (!log.is(ForestBlocks.PALE_OAK_LOG.get()) || log.getValue(RotatedPillarBlock.AXIS) != axis) return false;
        }
        return true;
    }
    @Override public boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart ? heart.outputSignal() : 0;
    }
    @Override public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
        return state.getValue(NATURAL) ? 20 + random.nextInt(5) : 0;
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart) heart.removeProtector();
        super.onRemove(state, level, pos, next, moving);
    }
}

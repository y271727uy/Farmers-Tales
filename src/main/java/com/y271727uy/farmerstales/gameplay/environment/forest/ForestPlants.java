package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IForgeShearable;

public final class ForestPlants {
    private ForestPlants() {}

    public static class PaleMoss extends MossBlock {
        public PaleMoss() { super(BlockBehaviour.Properties.copy(Blocks.MOSS_BLOCK)); }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolder(ForestWorldgen.featureKey("pale_moss_patch"))
                    .ifPresent(feature -> feature.value().place(level, level.getChunkSource().getGenerator(), random, pos.above()));
        }
    }

    public static class HangingMoss extends Block implements BonemealableBlock, IForgeShearable {
        public static final BooleanProperty TIP = BooleanProperty.create("tip");

        public HangingMoss() {
            super(BlockBehaviour.Properties.copy(Blocks.MOSS_CARPET).noCollission().instabreak().noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(TIP, true));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(TIP); }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            var above = level.getBlockState(pos.above());
            return above.is(this) || above.is(net.minecraft.tags.BlockTags.LEAVES)
                    || above.isFaceSturdy(level, pos.above(), Direction.DOWN);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction direction, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            if (!canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
            return direction == Direction.DOWN ? state.setValue(TIP, !other.is(this)) : state;
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(TIP, !context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(2, 0, 2, 14, 16, 14); }
        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return level.isEmptyBlock(bottom(level, pos).below()); }
        @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }

        private BlockPos bottom(LevelReader level, BlockPos pos) {
            var current = pos;
            while (current.getY() > level.getMinBuildHeight() && level.getBlockState(current.below()).is(this)) current = current.below();
            return current;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            var end = bottom(level, pos);
            if (level.isEmptyBlock(end.below())) level.setBlockAndUpdate(end.below(), defaultBlockState());
        }
    }

    public static class Eyeblossom extends FlowerBlock {
        private final boolean open;

        public Eyeblossom(boolean open, MobEffect effect, int seconds) {
            super(effect, seconds, BlockBehaviour.Properties.copy(Blocks.POPPY).randomTicks());
            this.open = open;
        }

        public boolean change(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,boolean propagated) {
            if(!level.dimensionType().natural()||level.isNight()==open)return false;
            level.setBlockAndUpdate(pos,(open?ForestBlocks.CLOSED_EYEBLOSSOM:ForestBlocks.OPEN_EYEBLOSSOM).get().defaultBlockState());
            level.playSound(null,pos,NatureSounds.get("block.eyeblossom."+(open?"close":"open")+(propagated?"":"_long")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos,net.minecraft.world.level.gameevent.GameEvent.Context.of(state));
            var color=new org.joml.Vector3f(open?0.62F:1F,open?0.68F:0.52F,open?0.7F:0.1F);
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color,0.8F),pos.getX()+0.5,pos.getY()+0.45,pos.getZ()+0.5,8,0.2,0.2,0.2,0.02);
            for(var near:BlockPos.betweenClosed(pos.offset(-3,-2,-3),pos.offset(3,2,3)))if(!near.equals(pos)&&level.getBlockState(near).is(this)){
                double distance=Math.sqrt(pos.distSqr(near));int min=(int)(distance*5),max=(int)(distance*10);
                level.scheduleTick(near,this,min+random.nextInt(max-min+1));
            }
            return true;
        }
        @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){change(state,level,pos,random,false);}
        @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){change(state,level,pos,random,true);}
        @Override public void entityInside(BlockState state,Level level,BlockPos pos,Entity entity){
            if(open&&!level.isClientSide&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&entity instanceof net.minecraft.world.entity.animal.Bee bee&&!bee.hasEffect(net.minecraft.world.effect.MobEffects.POISON))
                bee.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,25));
        }
    }

    public static class RedShrub extends BushBlock implements BonemealableBlock {
        public RedShrub() { super(BlockBehaviour.Properties.copy(Blocks.DEAD_BUSH).replaceable().offsetType(BlockBehaviour.OffsetType.XZ)); }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            var offset = state.getOffset(level, pos);
            return Block.box(2, 0, 2, 14, 10, 14).move(offset.x, offset.y, offset.z);
        }
        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var target = pos.relative(direction);
                if (level.isEmptyBlock(target) && state.canSurvive(level, target)) return true;
            }
            return false;
        }
        @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            int start = random.nextInt(4);
            for (int i = 0; i < 4; i++) {
                var target = pos.relative(Direction.from2DDataValue((start + i) % 4));
                if (level.isEmptyBlock(target) && state.canSurvive(level, target)) {
                    level.setBlockAndUpdate(target, defaultBlockState());
                    return;
                }
            }
        }
    }

    public static class LeafLitter extends PinkPetalsBlock {
        public LeafLitter() { super(BlockBehaviour.Properties.copy(Blocks.PINK_PETALS).sound(NatureSounds.litter()).replaceable()); }
        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) { return state.isFaceSturdy(level, pos, Direction.UP); }
        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return false; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {}
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Block.box(0, 0, 0, 16, 1, 16); }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 60; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 60; }
    }

    public static class ShelfMushroom extends HorizontalDirectionalBlock implements BonemealableBlock {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 1);

        public ShelfMushroom() {
            super(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB).strength(0.2F).sound(SoundType.FUNGUS).noOcclusion());
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, AGE); }
        @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            var facing = state.getValue(FACING);
            var support = pos.relative(facing.getOpposite());
            return level.getBlockState(support).isFaceSturdy(level, support, facing);
        }
        @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
            for (var direction : context.getNearestLookingDirections()) {
                if (direction.getAxis().isHorizontal()) {
                    var state = defaultBlockState().setValue(FACING, direction.getOpposite());
                    if (canSurvive(state, context.getLevel(), context.getClickedPos())) return state;
                }
            }
            return null;
        }
        @Override public BlockState updateShape(BlockState state, Direction direction, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
            return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
        }
        @Override public BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
        @Override public BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            boolean large = state.getValue(AGE) == 1;
            double low = large ? 5 : 8, high = large ? 10 : 11, edge = large ? 2 : 9;
            return switch (state.getValue(FACING)) {
                case NORTH -> Block.box(2, low, edge, 14, high, 16);
                case SOUTH -> Block.box(2, low, 0, 14, high, 16 - edge);
                case EAST -> Block.box(0, low, 2, 16 - edge, high, 14);
                default -> Block.box(edge, low, 2, 16, high, 14);
            };
        }
        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return state.getValue(AGE) == 0; }
        @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) { level.setBlockAndUpdate(pos, state.setValue(AGE, 1)); }
        @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) { super.fallOn(level, state, pos, entity, distance * 0.5F); }
        @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
            if (entity.isSuppressingBounce()) { super.updateEntityAfterFallOn(level, entity); return; }
            var velocity = entity.getDeltaMovement();
            if (velocity.y < 0) entity.setDeltaMovement(velocity.x, -velocity.y * 0.66, velocity.z);
        }
    }
}

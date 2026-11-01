package com.y271727uy.farmerstales.gameplay.environment;

import com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SpringPlants {
    private SpringPlants() {}

    public static class FireflyBush extends BushBlock implements BonemealableBlock {
        public FireflyBush() {
            super(BlockBehaviour.Properties.copy(Blocks.GRASS).lightLevel(state -> 2));
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Block.box(2, 0, 2, 14, 16, 14);
        }

        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (level.isNight() && random.nextInt(30) == 0
                    && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY()) {
                level.playLocalSound(pos, SpringNatureEffects.FIREFLY_BUSH_IDLE.get(), SoundSource.AMBIENT, 1, 1, false);
            }
            if (level.getMaxLocalRawBrightness(pos) <= 13 && random.nextDouble() <= 0.7) {
                level.addParticle(SpringNatureEffects.FIREFLY.get(), pos.getX() + random.nextDouble() * 10 - 5,
                        pos.getY() + random.nextDouble() * 5, pos.getZ() + random.nextDouble() * 10 - 5, 0, 0, 0);
            }
        }

        @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                var next = pos.relative(direction);
                if (level.isEmptyBlock(next) && state.canSurvive(level, next)) return true;
            }
            return false;
        }
        @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return true; }
        @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            int start = random.nextInt(4);
            for (int i = 0; i < 4; i++) {
                var next = pos.relative(Direction.from2DDataValue((start + i) % 4));
                if (level.isEmptyBlock(next) && state.canSurvive(level, next)) {
                    level.setBlockAndUpdate(next, defaultBlockState());
                    return;
                }
            }
        }
    }

    public static class CactusFlower extends BushBlock {
        public CactusFlower() { super(BlockBehaviour.Properties.copy(Blocks.POPPY)); }

        @Override protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return state.is(Blocks.CACTUS) || state.is(Blocks.FARMLAND)
                    || state.isFaceSturdy(level, pos, Direction.UP, SupportType.CENTER);
        }

        @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return Block.box(2, 0, 2, 14, 10, 14);
        }
    }

    /** Adds the newer age-8 flowering chance while retaining vanilla's short-cactus growth. */
    public static boolean tickCactusFlower(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1) || !level.isEmptyBlock(pos.above())) return false;
        int height = 1;
        while (level.getBlockState(pos.below(height)).is(Blocks.CACTUS)) height++;
        int age = state.getValue(CactusBlock.AGE);
        if (height >= 3 && age >= 15) return true;
        boolean flowering = age == 8 && NaturalEnvironmentBlocks.CACTUS_FLOWER.get().defaultBlockState().canSurvive(level, pos.above());
        if (flowering) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!level.isEmptyBlock(pos.above().relative(direction))) { flowering = false; break; }
            }
        }
        flowering = flowering && random.nextDouble() < (height >= 3 ? 0.25 : 0.1);
        if (height < 3 && !flowering) return false;
        if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos.above(), state, true)) {
            level.setBlock(pos, state.setValue(CactusBlock.AGE, Math.min(15, age + 1)), 4);
            if (flowering) level.setBlockAndUpdate(pos.above(), NaturalEnvironmentBlocks.CACTUS_FLOWER.get().defaultBlockState());
            net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
        }
        return true;
    }
}

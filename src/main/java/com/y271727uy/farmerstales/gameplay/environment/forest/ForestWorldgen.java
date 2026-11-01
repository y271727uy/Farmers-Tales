package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.*;
import net.minecraft.world.level.levelgen.feature.treedecorators.*;
import net.minecraft.world.level.levelgen.feature.trunkplacers.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class ForestWorldgen {
    private static final DeferredRegister<TrunkPlacerType<?>> TRUNKS = DeferredRegister.create(Registries.TRUNK_PLACER_TYPE, FTMod.MODID);
    private static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE = DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, FTMod.MODID);
    private static final DeferredRegister<TreeDecoratorType<?>> DECORATORS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, FTMod.MODID);
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, FTMod.MODID);
    public static final RegistryObject<TrunkPlacerType<PoplarTrunk>> POPLAR_TRUNK = TRUNKS.register("poplar_trunk_placer", () -> new TrunkPlacerType<>(PoplarTrunk.CODEC));
    public static final RegistryObject<FoliagePlacerType<PoplarFoliage>> POPLAR_FOLIAGE = FOLIAGE.register("poplar_foliage_placer", () -> new FoliagePlacerType<>(PoplarFoliage.CODEC));
    public static final RegistryObject<TreeDecoratorType<ForestDecorator>> DECORATION = DECORATORS.register("forest_decoration", () -> new TreeDecoratorType<>(ForestDecorator.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FALLEN_POPLAR = FEATURES.register("fallen_poplar", FallenPoplar::new);

    public static final RegistryObject<Feature<FallenTreeFeature.Configuration>> FALLEN_TREE = FEATURES.register("fallen_tree", FallenTreeFeature::new);
    public static final RegistryObject<TreeDecoratorType<FallenTreeFeature.LeafLitterDecorator>> LEAF_LITTER_DECORATOR = DECORATORS.register(
            "leaf_litter", () -> new TreeDecoratorType<>(FallenTreeFeature.LeafLitterDecorator.CODEC));

    private ForestWorldgen() {}

    public static ResourceKey<ConfiguredFeature<?, ?>> featureKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath(FTMod.MODID, name));
    }

    public static void register(IEventBus bus) {
        TRUNKS.register(bus); FOLIAGE.register(bus); DECORATORS.register(bus); FEATURES.register(bus);
    }

    /** Branching trunks keep distant foliage supported through vanilla leaf-distance updates. */
    public static class PoplarTrunk extends TrunkPlacer {
        public static final Codec<PoplarTrunk> CODEC = RecordCodecBuilder.create(instance -> trunkPlacerParts(instance).apply(instance, PoplarTrunk::new));
        public PoplarTrunk(int height, int randomA, int randomB) { super(height, randomA, randomB); }
        @Override protected TrunkPlacerType<?> type() { return POPLAR_TRUNK.get(); }

        @Override
        public List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader level, BiConsumer<BlockPos, BlockState> setter,
                RandomSource random, int height, BlockPos origin, TreeConfiguration config) {
            setDirtAt(level, setter, random, origin.below(), config);
            for (int y = 0; y < height; y++) placeLog(level, setter, random, origin.above(y), config);
            int branches = 1 + random.nextInt(4);
            int start = random.nextInt(4);
            for (int branch = 0; branch < branches; branch++) {
                var direction = Direction.from2DDataValue((start + branch) % 4);
                int y = Math.max(3, height - 4 - random.nextInt(2));
                int length = 2 + random.nextInt(2);
                for (int step = 1; step <= length; step++) {
                    var position = origin.above(y + step / 2).relative(direction, step);
                    placeLog(level, setter, random, position, config,
                            state -> state.setValue(RotatedPillarBlock.AXIS, direction.getAxis()));
                }
            }
            return List.of(new FoliagePlacer.FoliageAttachment(origin.above(height), 0, false));
        }
    }

    /** Tall rounded crowns with irregular edges, adapted to the old foliage-placer API. */
    public static class PoplarFoliage extends FoliagePlacer {
        public static final Codec<PoplarFoliage> CODEC = RecordCodecBuilder.create(instance -> foliagePlacerParts(instance)
                .and(Codec.intRange(3, 8).fieldOf("height").forGetter(placer -> placer.height))
                .apply(instance, PoplarFoliage::new));
        private final int height;
        public PoplarFoliage(IntProvider radius, IntProvider offset, int height) { super(radius, offset); this.height = height; }
        @Override protected FoliagePlacerType<?> type() { return POPLAR_FOLIAGE.get(); }
        @Override public int foliageHeight(RandomSource random, int trunkHeight, TreeConfiguration config) { return height; }

        @Override
        protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random, TreeConfiguration config,
                int trunkHeight, FoliageAttachment attachment, int foliageHeight, int radius, int offset) {
            for (int row = 1; row >= -foliageHeight; row--) {
                int rowRadius = row >= 0 ? 1 + (1 - row) : Math.min(radius, 2 + Math.min(-row, foliageHeight + row));
                placeLeavesRow(level, setter, random, config, attachment.pos(), rowRadius, row + offset, false);
            }
        }

        @Override
        protected boolean shouldSkipLocation(RandomSource random, int x, int y, int z, int radius, boolean doubleTrunk) {
            return x + z > radius + 1 || (x + z >= radius && random.nextFloat() < 0.15F);
        }
    }

    public static class ForestDecorator extends TreeDecorator {
        public static final Codec<ForestDecorator> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(Codec.BOOL.fieldOf("pale").forGetter(value -> value.pale)).apply(instance, ForestDecorator::new));
        private final boolean pale;
        public ForestDecorator(boolean pale) { this.pale = pale; }
        @Override protected TreeDecoratorType<?> type() { return DECORATION.get(); }

        @Override
        public void place(Context context) {
            if (context.logs().isEmpty()) return;
            var random = context.random();
            var base = context.logs().get(0);
            if (pale) {
                // Only natural-tree configurations include this decorator; saplings never grow hearts.
                if (random.nextFloat() < 0.1F) {
                    var candidates = context.logs().stream().filter(pos ->
                            context.level().isStateAtPosition(pos.above(), state -> state.is(ForestBlocks.PALE_OAK_LOG.get())
                                    && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y)
                            && context.level().isStateAtPosition(pos.below(), state -> state.is(ForestBlocks.PALE_OAK_LOG.get())
                                    && state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y)).toList();
                    if (!candidates.isEmpty()) context.setBlock(candidates.get(random.nextInt(candidates.size())),
                            com.y271727uy.farmerstales.gameplay.creaking.CreakingContent.HEART.get().defaultBlockState()
                                    .setValue(com.y271727uy.farmerstales.gameplay.creaking.CreakingHeartBlock.NATURAL, true));
                }
                for (var leaf : context.leaves()) {
                    if (random.nextFloat() >= 0.15F || !context.isAir(leaf.below())) continue;
                    int length = 1 + random.nextInt(4);
                    for (int i = 1; i <= length && context.isAir(leaf.below(i)); i++) {
                        boolean tip = i == length || !context.isAir(leaf.below(i + 1));
                        context.setBlock(leaf.below(i), ForestBlocks.PALE_HANGING_MOSS.get().defaultBlockState().setValue(ForestPlants.HangingMoss.TIP, tip));
                    }
                }
            } else {
                for (var log : context.logs()) {
                    if (log.getY() < base.getY() + 1 || log.getY() > base.getY() + 5 || random.nextFloat() >= 0.4F) continue;
                    var direction = Direction.from2DDataValue(random.nextInt(4));
                    var position = log.relative(direction);
                    if (context.isAir(position)) context.setBlock(position, ForestBlocks.SHELF_MUSHROOM.get().defaultBlockState()
                            .setValue(ForestPlants.ShelfMushroom.FACING, direction).setValue(ForestPlants.ShelfMushroom.AGE, random.nextInt(2)));
                }
            }
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                if (x * x + z * z > 20 || random.nextFloat() > (pale ? 0.8F : 0.55F)) continue;
                for (int y = 1; y >= -3; y--) {
                    var ground = base.offset(x, y, z);
                    if (context.level().isStateAtPosition(ground, state -> state.is(BlockTags.DIRT)) && context.isAir(ground.above())) {
                        if (pale) context.setBlock(ground, ForestBlocks.PALE_MOSS_BLOCK.get().defaultBlockState());
                        var cover = pale ? ForestBlocks.PALE_MOSS_CARPET.get().defaultBlockState()
                                : ForestBlocks.LEAF_LITTER.get().defaultBlockState().setValue(PinkPetalsBlock.AMOUNT, 1 + random.nextInt(4))
                                        .setValue(PinkPetalsBlock.FACING, Direction.from2DDataValue(random.nextInt(4)));
                        if (pale && context.level() instanceof net.minecraft.world.level.LevelAccessor mossLevel) {
                            ((PaleMossCarpet)ForestBlocks.PALE_MOSS_CARPET.get()).placeAt(mossLevel,ground.above(),random,context::setBlock);
                        } else context.setBlock(ground.above(), cover);
                        break;
                    }
                }
            }
        }
    }

    public static class FallenPoplar extends Feature<NoneFeatureConfiguration> {
        public FallenPoplar() { super(NoneFeatureConfiguration.CODEC); }
        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            var level = context.level();
            var random = context.random();
            var origin = context.origin();
            var direction = Direction.from2DDataValue(random.nextInt(4));
            int length = 4 + random.nextInt(4);
            List<BlockPos> positions = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                var pos = origin.relative(direction, i);
                if (!level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()
                        || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
                positions.add(pos);
            }
            for (var pos : positions) {
                level.setBlock(pos, ForestBlocks.POPLAR_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, direction.getAxis()), 2);
                var side = direction.getClockWise();
                var mushroom = pos.relative(side);
                if (random.nextFloat() < 0.8F && level.isEmptyBlock(mushroom)) {
                    level.setBlock(mushroom, ForestBlocks.SHELF_MUSHROOM.get().defaultBlockState()
                            .setValue(ForestPlants.ShelfMushroom.FACING, side).setValue(ForestPlants.ShelfMushroom.AGE, random.nextInt(2)), 2);
                }
            }
            var stump = origin.relative(direction.getOpposite(), 2);
            if (level.getBlockState(stump).canBeReplaced() && level.getFluidState(stump).isEmpty()
                    && level.getBlockState(stump.below()).isFaceSturdy(level, stump.below(), Direction.UP)) {
                level.setBlock(stump, ForestBlocks.POPLAR_LOG.get().defaultBlockState(), 2);
            }
            return true;
        }
    }
}

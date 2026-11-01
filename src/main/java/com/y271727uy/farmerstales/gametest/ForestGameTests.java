package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestBiomes;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestPlants;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class ForestGameTests {
    @GameTest(template = "crop_stand_empty")
    public static void paleMossSupportsForestPlantsAndNewPlantsDoNotObstructTrees(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), ForestBlocks.PALE_MOSS_BLOCK.get().defaultBlockState());
        for (var block : new Block[]{ForestBlocks.PALE_OAK_SAPLING.get(), ForestBlocks.POPLAR_SAPLING.get(),
                ForestBlocks.CLOSED_EYEBLOSSOM.get(), ForestBlocks.OPEN_EYEBLOSSOM.get(), ForestBlocks.RED_SHRUB.get(),
                com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks.BUSH.get(), Blocks.GRASS, Blocks.FERN}) {
            helper.assertTrue(block.defaultBlockState().canSurvive(level, pos), "Pale moss must support " + block);
        }
        for (var block : new Block[]{com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks.BUSH.get(),
                com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get(),
                com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks.TALL_DRY_GRASS.get()}) {
            helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.REPLACEABLE_BY_TREES),
                    "Natural decoration must not obstruct tree generation: " + block);
        }
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", timeoutTicks = 400)
    public static void forestsAreNaturallyLocatableAndKeepVanillaForests(GameTestHelper helper) {
        var level = helper.getLevel();
        var source = (MultiNoiseBiomeSource) level.getChunkSource().getGenerator().getBiomeSource();
        helper.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(ForestBiomes.PALE_GARDEN)), "Pale Garden must be in the live biome source");
        helper.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(ForestBiomes.DAPPLED_FOREST)), "Dappled Forest must be in the live biome source");
        helper.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(Biomes.DARK_FOREST)), "Dark Forest must remain available");
        helper.assertTrue(source.possibleBiomes().stream().anyMatch(b -> b.is(Biomes.TAIGA)), "Taiga must remain available");
        for (var key : java.util.List.of(ForestBiomes.PALE_GARDEN, ForestBiomes.DAPPLED_FOREST)) {
            var found = source.findBiomeHorizontal(0, 80, 0, 16000, 32, biome -> biome.is(key), level.random,
                    true, level.getChunkSource().randomState().sampler());
            helper.assertTrue(found != null, "Vanilla noise-based biome search must find " + key.location());
            // Generate a real remote chunk in each located forest: this exercises
            // biome-source installation, feature sorting and the full vegetation stage.
            var chunk = level.getChunkAt(found.getFirst());
            helper.assertTrue(chunk != null, "The located forest chunk must generate");
        }
        helper.succeed();
    }

    private static void ground(GameTestHelper helper) {
        for (int x = 1; x < 31; x++) for (int z = 1; z < 31; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.GRASS_BLOCK);
    }

    @GameTest(template = "forest_empty")
    public static void paleOakAndPoplarSaplingsRegrowTheirOwnTrees(GameTestHelper helper) {
        ground(helper);
        var level = helper.getLevel();
        var pale = ForestBlocks.PALE_OAK_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        var root = new BlockPos(7, 2, 7);
        helper.setBlock(root, pale);
        ((SaplingBlock) pale.getBlock()).advanceTree(level, helper.absolutePos(root), pale, level.random);
        helper.assertBlockPresent(pale.getBlock(), root); // A single pale sapling cannot form a giant tree.
        for (int x = 0; x < 2; x++) for (int z = 0; z < 2; z++) helper.setBlock(root.offset(x, 0, z), pale);
        ((SaplingBlock) pale.getBlock()).advanceTree(level, helper.absolutePos(root), pale, level.random);
        helper.assertBlockPresent(ForestBlocks.PALE_OAK_LOG.get(), root);
        var poplar = ForestBlocks.POPLAR_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, 1);
        var poplarRoot = new BlockPos(23, 2, 23);
        helper.setBlock(poplarRoot, poplar);
        ((SaplingBlock) poplar.getBlock()).advanceTree(level, helper.absolutePos(poplarRoot), poplar, level.random);
        helper.assertBlockPresent(ForestBlocks.POPLAR_LOG.get(), poplarRoot);
        helper.succeed();
    }

    @GameTest(template = "forest_empty")
    public static void allThreePoplarColorsGenerateSupportedCanopies(GameTestHelper helper) {
        ground(helper);
        var level = helper.getLevel();
        var colors = new String[]{"red", "orange", "yellow"};
        var positions = new BlockPos[]{new BlockPos(7, 2, 7), new BlockPos(23, 2, 7), new BlockPos(7, 2, 23)};
        for (int i = 0; i < colors.length; i++) {
            var origin = helper.absolutePos(positions[i]);
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .getHolderOrThrow(ForestWorldgen.featureKey(colors[i] + "_poplar_bonemeal")).value();
            helper.assertTrue(feature.place(level, level.getChunkSource().getGenerator(), level.random, origin), "Poplar tree must place");
            var leaves = ForestBlocks.ALL.get(colors[i] + "_poplar_leaves").get();
            int supported = 0;
            for (var pos : BlockPos.betweenClosed(origin.offset(-5, 0, -5), origin.offset(5, 16, 5))) {
                var state = level.getBlockState(pos);
                if (state.is(leaves) && state.getValue(LeavesBlock.DISTANCE) < 7) supported++;
            }
            helper.assertTrue(supported > 20, "Each poplar color must have a supported leaf canopy");
        }
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty")
    public static void forestPlantsRespondToDaylightBoneMealAndSupport(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        long previousTime = level.getDayTime();
        try {
            level.setDayTime(18000);
            level.updateSkyBrightness();
            var closed = ForestBlocks.CLOSED_EYEBLOSSOM.get().defaultBlockState();
            level.setBlockAndUpdate(pos, closed);
            closed.randomTick(level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).is(ForestBlocks.OPEN_EYEBLOSSOM.get()), "Eyeblossom must open at night");
            level.setDayTime(6000);
            level.updateSkyBrightness();
            level.getBlockState(pos).randomTick(level, pos, level.random);
            helper.assertTrue(level.getBlockState(pos).is(closed.getBlock()), "Eyeblossom must close during daytime");
        } finally { level.setDayTime(previousTime); level.updateSkyBrightness(); }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), ForestBlocks.PALE_OAK_LEAVES.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        var moss = ForestBlocks.PALE_HANGING_MOSS.get().defaultBlockState();
        level.setBlockAndUpdate(pos, moss);
        ((BonemealableBlock) moss.getBlock()).performBonemeal(level, level.random, pos, moss);
        helper.assertTrue(level.getBlockState(pos.below()).is(moss.getBlock()), "Hanging moss must extend downwards");
        helper.assertTrue(!level.getBlockState(pos).getValue(ForestPlants.HangingMoss.TIP), "Only the bottom moss block has a tip");
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(level.isEmptyBlock(pos) && level.isEmptyBlock(pos.below()), "Unsupported moss must break down the chain");
        level.setBlockAndUpdate(pos.south(), ForestBlocks.POPLAR_LOG.get().defaultBlockState());
        var mushroom = ForestBlocks.SHELF_MUSHROOM.get().defaultBlockState().setValue(ForestPlants.ShelfMushroom.FACING, Direction.NORTH);
        level.setBlockAndUpdate(pos, mushroom);
        ((BonemealableBlock) mushroom.getBlock()).performBonemeal(level, level.random, pos, mushroom);
        var grown = level.getBlockState(pos);
        var drops = Block.getDrops(grown, level, pos, null, null, ItemStack.EMPTY);
        helper.assertTrue(drops.size() == 1 && drops.get(0).getCount() == 2, "Large shelf mushrooms must drop two items");
        helper.assertTrue(!grown.getCollisionShape(level, pos).isEmpty(), "Shelf mushroom must support an entity for bouncing");
        var sheep = EntityType.SHEEP.create(level);
        sheep.setDeltaMovement(0, -1, 0);
        mushroom.getBlock().updateEntityAfterFallOn(level, sheep);
        helper.assertTrue(sheep.getDeltaMovement().y > 0, "Shelf mushroom must bounce falling entities");
        helper.succeed();
    }
}

package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class SpringNatureGameTests {
    @GameTest(template = "crop_stand_empty")
    public static void wildflowersGrowToFourDropTheirCountAndMultiply(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        var block = (PinkPetalsBlock) NaturalEnvironmentBlocks.WILDFLOWERS.get();
        var state = block.defaultBlockState().setValue(PinkPetalsBlock.FACING, Direction.WEST);
        level.setBlockAndUpdate(pos, state);
        for (int amount = 1; amount <= 4; amount++) {
            state = level.getBlockState(pos);
            helper.assertTrue(state.getValue(PinkPetalsBlock.AMOUNT) == amount, "Wildflowers must accumulate one bunch at a time");
            var loot = Block.getDrops(state, level, pos, null, null, ItemStack.EMPTY);
            helper.assertTrue(loot.size() == 1 && loot.get(0).getCount() == amount, "Wildflower drops must match placed amount");
            block.performBonemeal(level, level.random, pos, state);
        }
        helper.assertTrue(level.getBlockState(pos).getValue(PinkPetalsBlock.FACING) == Direction.WEST, "Bone meal must preserve facing");
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2))
                .stream().anyMatch(entity -> entity.getItem().is(block.asItem())), "Bone meal on four bunches must produce another item");
        helper.succeed();
    }

    @GameTest(template = "forest_empty")
    public static void fireflyBushGeneratesOnlyAtWaterAndCanBePropagated(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 1; x < 31; x++) for (int z = 1; z < 31; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.DIRT);
        for (int z = 3; z < 29; z++) helper.setBlock(new BlockPos(15, 1, z), Blocks.WATER);
        var origin = helper.absolutePos(new BlockPos(16, 2, 16));
        var bush = NaturalEnvironmentBlocks.FIREFLY_BUSH.get();
        var patch = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ForestWorldgen.featureKey("patch_firefly_bush")).value();
        for (int i = 0; i < 12; i++) patch.place(level, level.getChunkSource().getGenerator(), RandomSource.create(2026L + i), origin);
        int count = 0;
        for (var pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(1, 2, 1)), helper.absolutePos(new BlockPos(30, 2, 30)))) {
            if (!level.getBlockState(pos).is(bush)) continue;
            count++;
            boolean nextToWater = false;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (level.getFluidState(pos.below().relative(direction)).is(net.minecraft.tags.FluidTags.WATER)) nextToWater = true;
            }
            helper.assertTrue(nextToWater, "Naturally generated firefly bushes must stay beside water");
        }
        helper.assertTrue(count > 0, "Water-edge fixture must produce firefly bushes");
        helper.assertTrue(bush.defaultBlockState().getLightEmission() == 2, "Firefly bushes must emit light level 2");
        var parent = helper.absolutePos(new BlockPos(5, 2, 5));
        level.setBlockAndUpdate(parent, bush.defaultBlockState());
        ((BonemealableBlock) bush).performBonemeal(level, RandomSource.create(1), parent, bush.defaultBlockState());
        int children = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) if (level.getBlockState(parent.relative(direction)).is(bush)) children++;
        helper.assertTrue(children == 1, "Bone meal must propagate one firefly bush on ordinary soil");
        helper.succeed();
    }

    @GameTest(template = "forest_empty")
    public static void cactusGrowthFlowersAtBothHeightsAndRetainsVanillaGrowth(GameTestHelper helper) {
        var level = helper.getLevel();
        var flower = NaturalEnvironmentBlocks.CACTUS_FLOWER.get();
        int[] blooms = new int[2];
        for (int variant = 0; variant < 2; variant++) {
            int height = variant == 0 ? 1 : 3;
            var base = helper.absolutePos(new BlockPos(5 + variant * 10, 2, 5));
            level.setBlockAndUpdate(base.below(), Blocks.SAND.defaultBlockState());
            for (int y = 0; y < height; y++) level.setBlockAndUpdate(base.above(y), Blocks.CACTUS.defaultBlockState());
            var top = base.above(height - 1);
            var random = RandomSource.create(271727);
            for (int attempt = 0; attempt < 256; attempt++) {
                level.setBlockAndUpdate(top.above(), Blocks.AIR.defaultBlockState());
                var state = Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE, 8);
                level.setBlock(top, state, 2);
                state.randomTick(level, top, random); // Exercise the real CactusBlock mixin.
                if (level.getBlockState(top.above()).is(flower)) blooms[variant]++;
            }
            level.setBlockAndUpdate(top.above(), Blocks.AIR.defaultBlockState());
            if (height == 3) {
                var mature = Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE, 15);
                level.setBlock(top, mature, 2);
                mature.randomTick(level, top, random);
                helper.assertTrue(level.isEmptyBlock(top.above()), "Three-block cacti must not grow a fourth cactus");
            } else {
                var mature = Blocks.CACTUS.defaultBlockState().setValue(CactusBlock.AGE, 15);
                level.setBlock(top, mature, 2);
                mature.randomTick(level, top, random);
                helper.assertTrue(level.getBlockState(top.above()).is(Blocks.CACTUS), "Ordinary age-15 cactus growth must remain intact");
            }
        }
        helper.assertTrue(blooms[0] > 0 && blooms[1] > blooms[0], "Short and tall cactus flowering chances must both work, with more blooms on tall cacti");
        var support = helper.absolutePos(new BlockPos(6, 2, 18));
        level.setBlockAndUpdate(support.below(), Blocks.FARMLAND.defaultBlockState());
        helper.assertTrue(flower.defaultBlockState().canSurvive(level, support), "Cactus flowers must be plantable on farmland");
        level.setBlockAndUpdate(support.below(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(!flower.defaultBlockState().canSurvive(level, support), "Cactus flowers must require support");
        helper.succeed();
    }
}

package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class NaturalEnvironmentGameTests {
    @GameTest(template = "crop_stand_empty")
    public static void plantsGrowAndShearWithoutSeedDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos.below(), Blocks.SAND.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.setBlockAndUpdate(pos.relative(direction).below(), Blocks.SAND.defaultBlockState());
            level.setBlockAndUpdate(pos.relative(direction), Blocks.AIR.defaultBlockState());
        }
        var shortGrass = NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get().defaultBlockState();
        helper.assertTrue(shortGrass.canSurvive(level, pos), "Dry grass must survive on sand");
        level.setBlockAndUpdate(pos, shortGrass);
        ((BonemealableBlock) shortGrass.getBlock()).performBonemeal(level, level.random, pos, shortGrass);
        var tallGrass = level.getBlockState(pos);
        helper.assertTrue(tallGrass.is(NaturalEnvironmentBlocks.TALL_DRY_GRASS.get()), "Bone meal must grow short dry grass");
        ((BonemealableBlock) tallGrass.getBlock()).performBonemeal(level, level.random, pos, tallGrass);
        int neighbours = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(direction)).is(shortGrass.getBlock())) neighbours++;
        }
        helper.assertTrue(neighbours == 1, "Tall dry grass must spread one short plant");
        for (var entry : new Block[]{shortGrass.getBlock(), tallGrass.getBlock(), NaturalEnvironmentBlocks.BUSH.get()}) {
            var drops = Block.getDrops(entry.defaultBlockState(), level, pos, null, null, new ItemStack(Items.SHEARS));
            helper.assertTrue(drops.size() == 1 && drops.get(0).is(entry.asItem()), "Shears must collect " + entry);
            helper.assertTrue(Block.getDrops(entry.defaultBlockState(), level, pos, null, null, ItemStack.EMPTY).isEmpty(),
                    "Empty hands must not produce plants or seeds");
        }
        var bush = NaturalEnvironmentBlocks.BUSH.get().defaultBlockState();
        helper.assertTrue(!bush.canSurvive(level, pos), "Bush must not survive on sand");
        level.setBlockAndUpdate(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        helper.assertTrue(bush.canSurvive(level, pos), "Bush must survive on grass");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty")
    public static void sheepEatDryGrassAndRespectMobGriefing(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var sheep = EntityType.SHEEP.create(level);
        helper.assertTrue(sheep != null, "Sheep must exist");
        sheep.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.setBlockAndUpdate(pos.below(), Blocks.SAND.defaultBlockState());
        boolean previous = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        try {
            for (boolean griefing : new boolean[]{true, false}) {
                level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(griefing, level.getServer());
                for (var grass : new Block[]{NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get(), NaturalEnvironmentBlocks.TALL_DRY_GRASS.get()}) {
                    level.setBlockAndUpdate(pos, grass.defaultBlockState());
                    sheep.setSheared(true);
                    var goal = new EatBlockGoal(sheep);
                    boolean canEat = false;
                    for (int i = 0; i < 100000 && !canEat; i++) canEat = goal.canUse();
                    helper.assertTrue(canEat, "Vanilla eating goal must accept dry grass on sand");
                    goal.start();
                    for (int i = 0; i < 40; i++) goal.tick();
                    helper.assertTrue(!sheep.isSheared(), "Eating dry grass must regrow wool");
                    helper.assertTrue(griefing ? level.isEmptyBlock(pos) : level.getBlockState(pos).is(grass),
                            "Eating must respect mobGriefing");
                }
            }
        } finally {
            level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(previous, level.getServer());
        }
        helper.succeed();
    }
}

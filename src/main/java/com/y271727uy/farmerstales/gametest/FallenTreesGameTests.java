package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.environment.forest.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.*;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class FallenTreesGameTests {
    private static ConfiguredFeature<?,?> feature(GameTestHelper helper,String name) {
        return helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation(name));
    }
    private static void floor(GameTestHelper helper) {
        for(int x=0;x<32;x++)for(int z=0;z<32;z++)helper.setBlock(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);
    }
    @GameTest(template="forest_empty",timeoutTicks=200)
    public static void fourWoodsAndLongBirchHaveStumpsHorizontalLogsAndDecorations(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();var origin=helper.absolutePos(new BlockPos(15,2,15));
        var bounds=BlockPos.betweenClosed(origin.offset(-17,0,-17),origin.offset(17,1,17));
        String[] names={"oak","birch","spruce","jungle","super_birch"};
        Block[] blocks={Blocks.OAK_LOG,Blocks.BIRCH_LOG,Blocks.SPRUCE_LOG,Blocks.JUNGLE_LOG,Blocks.BIRCH_LOG};
        int[] min={2,3,4,2,3},max={5,6,8,9,13};
        int mushrooms=0,vines=0;boolean longBirch=false;
        for(int type=0;type<names.length;type++) {
            Set<Direction.Axis> axes=new HashSet<>();int generated=0;
            for(int seed=0;seed<24;seed++) {
                for(var pos:bounds) if(!level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                var configured=feature(helper,"farmerstales:fallen_"+names[type]+"_tree");
                helper.assertTrue(configured.place(level,level.getChunkSource().getGenerator(),RandomSource.create(2026+seed*7919L),origin),"Fallen tree must place on dry grass");
                helper.assertTrue(level.getBlockState(origin).is(blocks[type]) && level.getBlockState(origin).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y,"Stump must stand upright and use its wood type");
                int horizontal=0;
                for(var pos:bounds) {
                    var state=level.getBlockState(pos);
                    if(state.is(blocks[type]) && !pos.equals(origin)) {
                        var axis=state.getValue(RotatedPillarBlock.AXIS);axes.add(axis);horizontal++;
                        helper.assertTrue(axis!=Direction.Axis.Y && pos.getY()==origin.getY(),"Fallen trunk must lie horizontally on the ground");
                        int distance=Math.abs(pos.getX()-origin.getX())+Math.abs(pos.getZ()-origin.getZ());
                        helper.assertTrue(distance>=2,"Stump and fallen trunk must have an air gap");
                    }
                    if(state.is(Blocks.BROWN_MUSHROOM)||state.is(Blocks.RED_MUSHROOM)) {
                        mushrooms++;helper.assertTrue(level.getBlockState(pos.below()).is(blocks[type]),"Mushrooms must decorate the fallen log top");
                    }
                    if(state.is(Blocks.VINE)) {vines++;helper.assertTrue(type==0||type==3,"Only oak and jungle stumps receive vines");}
                }
                helper.assertTrue(horizontal>=min[type] && horizontal<=max[type],"Fallen trunk length must follow the official wood-specific range");
                if(type==4 && horizontal>9)longBirch=true;
                generated++;
            }
            helper.assertTrue(generated==24 && axes.size()==2,"Fallen trunks must support both horizontal axes across seeds");
        }
        helper.assertTrue(mushrooms>0 && vines>0 && longBirch,"Seed set must exercise mushrooms, vines and long birch");
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void fallenTreesRespectWaterObstaclesAndUnsupportedGaps(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();var origin=helper.absolutePos(new BlockPos(15,2,15));
        var tree=feature(helper,"farmerstales:fallen_oak_tree");
        level.setBlockAndUpdate(origin,Blocks.WATER.defaultBlockState());
        helper.assertTrue(!tree.place(level,level.getChunkSource().getGenerator(),RandomSource.create(1),origin) && level.getBlockState(origin).is(Blocks.WATER),"Fallen stump must not replace water");
        for(var pos:BlockPos.betweenClosed(origin.offset(-15,0,-15),origin.offset(15,0,15)))level.setBlock(pos,Blocks.DIAMOND_BLOCK.defaultBlockState(),2);
        level.setBlockAndUpdate(origin,Blocks.AIR.defaultBlockState());
        tree.place(level,level.getChunkSource().getGenerator(),RandomSource.create(12),origin);
        for(var pos:BlockPos.betweenClosed(origin.offset(-15,0,-15),origin.offset(15,0,15))) if(!pos.equals(origin))helper.assertTrue(level.getBlockState(pos).is(Blocks.DIAMOND_BLOCK),"Fallen log must never overwrite an obstacle");
        for(var pos:BlockPos.betweenClosed(origin.offset(-15,-1,-15),origin.offset(15,0,15)))level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        level.setBlockAndUpdate(origin.below(),Blocks.GRASS_BLOCK.defaultBlockState());
        tree.place(level,level.getChunkSource().getGenerator(),RandomSource.create(12),origin);
        int logs=0;for(var pos:BlockPos.betweenClosed(origin.offset(-15,0,-15),origin.offset(15,0,15)))if(level.getBlockState(pos).is(Blocks.OAK_LOG))logs++;
        helper.assertTrue(logs==1,"A cliff must leave just the stump rather than an unsupported floating trunk");
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void naturalForestTreesGrowLeafLitterWithoutChangingSaplings(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();var origin=helper.absolutePos(new BlockPos(15,2,15));
        for(String name:List.of("oak","birch","fancy_oak","dark_oak","oak_bees_0002","birch_bees_0002","fancy_oak_bees_0002")) {
            var config=(TreeConfiguration)feature(helper,"farmerstales:"+name+"_leaf_litter").config();
            var original=(TreeConfiguration)feature(helper,"minecraft:"+name).config();
            helper.assertTrue(original.decorators.stream().noneMatch(d->d instanceof FallenTreeFeature.LeafLitterDecorator),"Sapling and original tree configurations must stay unchanged");
            helper.assertTrue(config.decorators.size()==original.decorators.size()+1,"Natural tree clone must preserve bee and other decorators");
        }
        var tree=feature(helper,"farmerstales:oak_bees_0002_leaf_litter");
        helper.assertTrue(tree.place(level,level.getChunkSource().getGenerator(),RandomSource.create(2026),origin),"Natural oak fixture must generate");
        int count=0;Set<Integer> amounts=new HashSet<>();Set<Direction> directions=new HashSet<>();
        for(var pos:BlockPos.betweenClosed(origin.offset(-5,-1,-5),origin.offset(5,3,5))) {
            var state=level.getBlockState(pos);if(!state.is(ForestBlocks.LEAF_LITTER.get()))continue;
            count++;amounts.add(state.getValue(PinkPetalsBlock.AMOUNT));directions.add(state.getValue(PinkPetalsBlock.FACING));
            helper.assertTrue(state.canSurvive(level,pos) && !level.getBlockState(pos.below()).isAir(),"Litter must be supported by soil");
        }
        helper.assertTrue(count>=10 && amounts.size()>1 && directions.size()>1,"Natural forest tree must create a varied patch of leaf litter");
        // Direct decoration must not replace water, flowers, or build on unsupported spaces.
        var protectedPos=origin.offset(3,0,0);level.setBlockAndUpdate(protectedPos,Blocks.DANDELION.defaultBlockState());
        var water=origin.offset(-3,0,0);level.setBlockAndUpdate(water,Blocks.WATER.defaultBlockState());
        var context=new TreeDecorator.Context(level,level::setBlockAndUpdate,RandomSource.create(3),Set.of(origin),Set.of(),Set.of());
        new FallenTreeFeature.LeafLitterDecorator().place(context);
        helper.assertTrue(level.getBlockState(protectedPos).is(Blocks.DANDELION) && level.getBlockState(water).is(Blocks.WATER),"Litter decoration must preserve flowers and water");
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void selectorsReachFallenTreesAndKeepVanillaExclusions(GameTestHelper helper) {
        var level=helper.getLevel();
        for(String name:List.of("trees_birch_and_oak","dark_forest_vegetation","trees_flower_forest","trees_taiga","trees_jungle","birch_tall")) {
            var config=(RandomFeatureConfiguration)feature(helper,"minecraft:"+name).config();
            long fallen=config.features.stream().filter(f->f.feature.value().feature().value().feature()==ForestWorldgen.FALLEN_TREE.get()).count();
            helper.assertTrue(fallen>0,"Loaded vanilla selector must actually reference registered fallen trees: "+name);
            if(name.equals("trees_flower_forest"))helper.assertTrue(config.features.stream().filter(f->f.feature.value().feature().value().feature()==ForestWorldgen.FALLEN_TREE.get())
                    .allMatch(f->((FallenTreeFeature.Configuration)f.feature.value().feature().value().config()).trunkProvider().getState(RandomSource.create(0),BlockPos.ZERO).is(Blocks.BIRCH_LOG)),"Flower forest must only contain fallen birch");
        }
        var grove=(RandomFeatureConfiguration)feature(helper,"minecraft:trees_grove").config();
        helper.assertTrue(grove.features.stream().noneMatch(f->f.feature.value().feature().value().feature()==ForestWorldgen.FALLEN_TREE.get()),"Grove must retain vanilla fallen-tree exclusion");
        var placed=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var patch=placed.get(new ResourceLocation("farmerstales:patch_forest_leaf_litter"));
        var dark=level.registryAccess().registryOrThrow(Registries.BIOME).get(new ResourceLocation("minecraft:dark_forest"));
        helper.assertTrue(dark.getGenerationSettings().hasFeature(patch),"Dark forest must receive the extra ground litter patch");
        floor(helper);var origin=helper.absolutePos(new BlockPos(15,2,15));
        var configured=feature(helper,"farmerstales:patch_forest_leaf_litter");
        for(int seed=0;seed<8;seed++)configured.place(level,level.getChunkSource().getGenerator(),RandomSource.create(seed*7919L),origin);
        int leaves=0;for(var pos:BlockPos.betweenClosed(origin.offset(-8,0,-8),origin.offset(8,0,8)))if(level.getBlockState(pos).is(ForestBlocks.LEAF_LITTER.get()))leaves++;
        helper.assertTrue(leaves>0,"Ground patch must produce actual litter on grass");
        helper.succeed();
    }
}

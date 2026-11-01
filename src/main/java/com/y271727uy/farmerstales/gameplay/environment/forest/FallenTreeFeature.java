package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import java.util.*;

/** 26.3 fallen-tree layout adapted to the 1.20.1 feature API. */
public final class FallenTreeFeature extends Feature<FallenTreeFeature.Configuration> {
    public record Configuration(BlockStateProvider trunkProvider, IntProvider logLength, boolean vines) implements FeatureConfiguration {
        public static final Codec<Configuration> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(Configuration::trunkProvider),
                IntProvider.codec(3,16).fieldOf("log_length").forGetter(Configuration::logLength),
                Codec.BOOL.optionalFieldOf("stump_vines",false).forGetter(Configuration::vines)).apply(instance,Configuration::new));
    }
    public FallenTreeFeature() { super(Configuration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<Configuration> context) {
        var level=context.level();var origin=context.origin();var random=context.random();var config=context.config();
        // Placement selectors check sapling survival too. Validate here for direct /place and uneven terrain.
        if (!free(level,origin) || !supported(level,origin) || !level.getBlockState(origin.below()).is(BlockTags.DIRT)) return false;
        var direction=Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int length=config.logLength.sample(random)-2;
        var start=origin.relative(direction,2+random.nextInt(2)).above();
        boolean found=false;
        for (int i=0;i<6;i++,start=start.below()) if (free(level,start) && supported(level,start)) {found=true;break;}
        var logs=new ArrayList<BlockPos>();
        if(found) {
            int gap=0;
            for(int i=0;i<length;i++) {
                var pos=start.relative(direction,i);
                if(!free(level,pos) || (!supported(level,pos) && ++gap>2)) { logs.clear();break; }
                if(supported(level,pos))gap=0;
                logs.add(pos);
            }
        }
        // Like vanilla, an obstructed fallen trunk can leave just its stump. Never replace an obstacle.
        level.setBlock(origin,config.trunkProvider.getState(random,origin).setValue(RotatedPillarBlock.AXIS,Direction.Axis.Y),3);
        markAboveForPostProcessing(level,origin);
        if(config.vines) for(var side:Direction.Plane.HORIZONTAL) {
            var pos=origin.relative(side);
            if(random.nextInt(3)!=0 && level.isEmptyBlock(pos)) level.setBlock(pos,Blocks.VINE.defaultBlockState()
                    .setValue(VineBlock.getPropertyForFace(side.getOpposite()),true),19);
        }
        for(var pos:logs) {
            level.setBlock(pos,config.trunkProvider.getState(random,pos).setValue(RotatedPillarBlock.AXIS,direction.getAxis()),3);
            markAboveForPostProcessing(level,pos);
        }
        for(var pos:logs) if(random.nextFloat()<0.1F && level.isEmptyBlock(pos.above())) {
            var mushroom=random.nextInt(3)<2?Blocks.RED_MUSHROOM:Blocks.BROWN_MUSHROOM;
            level.setBlock(pos.above(),mushroom.defaultBlockState(),19);
        }
        return true;
    }
    private static boolean free(WorldGenLevel level,BlockPos pos) {
        return !level.isOutsideBuildHeight(pos) && level.hasChunkAt(pos) && level.getFluidState(pos).isEmpty()
                && TreeFeature.validTreePos(level,pos);
    }
    private static boolean supported(WorldGenLevel level,BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);
    }
    /** Used only by cloned natural trees; vanilla sapling configurations are untouched. */
    public static final class LeafLitterDecorator extends TreeDecorator {
        public static final Codec<LeafLitterDecorator> CODEC=Codec.unit(LeafLitterDecorator::new);
        @Override protected TreeDecoratorType<?> type() { return ForestWorldgen.LEAF_LITTER_DECORATOR.get(); }
        @Override public void place(Context context) {
            if(context.logs().isEmpty())return;
            int lowest=context.logs().get(0).getY();
            var roots=context.logs().stream().filter(pos->pos.getY()==lowest).toList();
            // A broad ground patch followed by a smaller pass filling gaps near the trunk.
            for(int pass=0;pass<2;pass++) for(int attempt=0;attempt<(pass==0?96:32);attempt++) {
                var root=roots.get(context.random().nextInt(roots.size()));int radius=pass==0?4:2;
                var column=root.offset(context.random().nextInt(radius*2+1)-radius,0,context.random().nextInt(radius*2+1)-radius);
                for(int y=2;y>=-2;y--) {
                    var ground=column.offset(0,y-1,0);var above=ground.above();
                    if(!context.level().isStateAtPosition(ground,s->s.is(BlockTags.DIRT)) || !context.isAir(above))continue;
                    context.setBlock(above,ForestBlocks.LEAF_LITTER.get().defaultBlockState()
                            .setValue(PinkPetalsBlock.AMOUNT,1+context.random().nextInt(pass==0?3:4))
                            .setValue(PinkPetalsBlock.FACING,Direction.Plane.HORIZONTAL.getRandomDirection(context.random())));
                    break;
                }
            }
        }
    }
}

package com.y271727uy.farmerstales.gameplay.environment.forest;
import com.y271727uy.farmerstales.all.*;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
public final class NatureAmbience {
    public static boolean desertGround(BlockState state){return state.is(Blocks.SAND)||state.is(Blocks.RED_SAND)||state.is(BlockTags.TERRACOTTA);}
    public static boolean dryGrassGround(Level level,BlockPos pos){return desertGround(level.getBlockState(pos.below()))&&desertGround(level.getBlockState(pos.below(2)));}
    public static boolean sandSurroundings(Level level,BlockPos pos){
        if(!desertGround(level.getBlockState(pos))||!level.isEmptyBlock(pos.above()))return false;
        int matching=0;
        for(var direction:Direction.Plane.HORIZONTAL){
            var neighbor=pos.relative(direction,8);if(!level.hasChunkAt(neighbor))continue;
            int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,neighbor.getX(),neighbor.getZ())-1;
            if(Math.abs(surface-pos.getY())<=5){if(desertGround(level.getBlockState(new BlockPos(neighbor.getX(),surface,neighbor.getZ()))))matching++;}
            else for(int dy=5;dy>=-5;dy--){var test=neighbor.offset(0,dy,0);if(desertGround(level.getBlockState(test))&&level.isEmptyBlock(test.above())){matching++;break;}}
        }
        return matching>=3;
    }
    public static void animate(BlockState state,Level level,BlockPos pos,RandomSource random){
        String sound=null;
        if(desertGround(state)&&random.nextInt(2100)==0&&sandSurroundings(level,pos))sound="block.sand.idle";
        else if((state.is(NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get())||state.is(NaturalEnvironmentBlocks.TALL_DRY_GRASS.get()))&&random.nextInt(200)==0&&dryGrassGround(level,pos))sound="block.dry_grass.ambient";
        else if(level.isNight()&&state.is(ForestBlocks.PALE_HANGING_MOSS.get())&&random.nextInt(500)==0)sound="block.pale_hanging_moss.idle";
        else if(level.isNight()&&state.is(ForestBlocks.OPEN_EYEBLOSSOM.get())&&level.getBlockState(pos.below()).is(ForestBlocks.PALE_MOSS_BLOCK.get())&&random.nextInt(700)==0)sound="block.eyeblossom.idle";
        if(sound!=null)level.playLocalSound(pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,NatureSounds.get(sound),SoundSource.AMBIENT,1,1,false);
    }
}

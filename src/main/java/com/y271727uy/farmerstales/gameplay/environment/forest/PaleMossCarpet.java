package com.y271727uy.farmerstales.gameplay.environment.forest;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import java.util.*;
import java.util.function.BooleanSupplier;

public final class PaleMossCarpet extends Block implements BonemealableBlock {
    public static final BooleanProperty BASE=BlockStateProperties.BOTTOM;
    public static final Map<Direction,EnumProperty<WallSide>> SIDES=Map.of(Direction.NORTH,BlockStateProperties.NORTH_WALL,Direction.SOUTH,BlockStateProperties.SOUTH_WALL,Direction.EAST,BlockStateProperties.EAST_WALL,Direction.WEST,BlockStateProperties.WEST_WALL);
    public PaleMossCarpet() {
        super(Properties.copy(Blocks.MOSS_CARPET).noOcclusion());
        var state=stateDefinition.any().setValue(BASE,true);
        for(var property:SIDES.values())state=state.setValue(property,WallSide.NONE);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BASE);SIDES.values().forEach(builder::add);}
    @Override public boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var below=level.getBlockState(pos.below());
        return state.getValue(BASE)?!below.isAir():below.is(this)&&below.getValue(BASE);
    }
    private boolean faces(BlockState state){return state.getValue(BASE)||SIDES.values().stream().anyMatch(p->state.getValue(p)!=WallSide.NONE);}
    public BlockState adjusted(BlockState state,BlockGetter level,BlockPos pos,boolean create){
        var above=level.getBlockState(pos.above());var below=level.getBlockState(pos.below());
        for(var direction:Direction.Plane.HORIZONTAL){
            var property=SIDES.get(direction);var neighbor=pos.relative(direction);
            boolean supported=MultifaceBlock.canAttachTo(level,direction,neighbor,level.getBlockState(neighbor));
            var side=supported?(create||state.getValue(BASE)?WallSide.LOW:state.getValue(property)):WallSide.NONE;
            if(side==WallSide.LOW){
                if(above.is(this)&&!above.getValue(BASE)&&above.getValue(property)!=WallSide.NONE)side=WallSide.TALL;
                if(!state.getValue(BASE)&&below.is(this)&&below.getValue(property)==WallSide.NONE)side=WallSide.NONE;
            }
            state=state.setValue(property,side);
        }
        return state;
    }
    private BlockState topper(BlockGetter level,BlockPos pos,BooleanSupplier chance){
        var above=level.getBlockState(pos.above());
        if((!above.is(this)||!above.getValue(BASE))&&(above.is(this)||above.canBeReplaced())&&above.getFluidState().isEmpty()){
            var state=adjusted(defaultBlockState().setValue(BASE,false),level,pos.above(),true);
            for(var p:SIDES.values())if(state.getValue(p)!=WallSide.NONE&&!chance.getAsBoolean())state=state.setValue(p,WallSide.NONE);
            if(faces(state)&&!state.equals(above))return state;
        }
        return Blocks.AIR.defaultBlockState();
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return adjusted(defaultBlockState(),context.getLevel(),context.getClickedPos(),true);}
    public void placeAt(LevelAccessor level,BlockPos pos,RandomSource random,java.util.function.BiConsumer<BlockPos,BlockState> setter){
        var state=adjusted(defaultBlockState(),level,pos,true);setter.accept(pos,state);
        var top=topper(level,pos,random::nextBoolean);if(!top.isAir()){setter.accept(pos.above(),top);setter.accept(pos,adjusted(state,level,pos,true));}
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        if(!old.is(this)&&state.getValue(BASE)&&!level.isClientSide)level.setBlock(pos,adjusted(state,level,pos,true),2);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack item){
        if(!level.isClientSide){var top=topper(level,pos,level.random::nextBoolean);if(!top.isAir())level.setBlockAndUpdate(pos.above(),top);}
    }
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        if(!canSurvive(state,level,pos))return Blocks.AIR.defaultBlockState();
        var result=adjusted(state,level,pos,false);return faces(result)?result:Blocks.AIR.defaultBlockState();
    }
    @Override public BlockState rotate(BlockState state,Rotation rotation){var result=state;for(var d:Direction.Plane.HORIZONTAL)result=result.setValue(SIDES.get(rotation.rotate(d)),state.getValue(SIDES.get(d)));return result;}
    @Override public BlockState mirror(BlockState state,Mirror mirror){var result=state;for(var d:Direction.Plane.HORIZONTAL)result=result.setValue(SIDES.get(mirror.mirror(d)),state.getValue(SIDES.get(d)));return result;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        VoxelShape shape=state.getValue(BASE)?Block.box(0,0,0,16,1,16):Shapes.empty();
        for(var d:Direction.Plane.HORIZONTAL){var side=state.getValue(SIDES.get(d));if(side==WallSide.NONE)continue;int height=side==WallSide.TALL?16:10;
            shape=Shapes.or(shape,switch(d){case NORTH->Block.box(0,0,0,16,height,1);case SOUTH->Block.box(0,0,15,16,height,16);case WEST->Block.box(0,0,0,1,height,16);default->Block.box(15,0,0,16,height,16);});}
        return shape.isEmpty()?Shapes.block():shape;
    }
    @Override public VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(BASE)?Block.box(0,0,0,16,1,16):Shapes.empty();}
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state,boolean client){return state.getValue(BASE)&&!topper(level,pos,()->true).isAir();}
    @Override public boolean isBonemealSuccess(Level level,RandomSource random,BlockPos pos,BlockState state){return true;}
    @Override public void performBonemeal(ServerLevel level,RandomSource random,BlockPos pos,BlockState state){var top=topper(level,pos,()->true);if(!top.isAir())level.setBlockAndUpdate(pos.above(),top);}
}

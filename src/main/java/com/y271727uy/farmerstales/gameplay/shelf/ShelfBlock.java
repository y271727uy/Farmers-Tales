package com.y271727uy.farmerstales.gameplay.shelf;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.*;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class ShelfBlock extends BaseEntityBlock implements SimpleWaterloggedBlock,SideChainPartBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED,WATERLOGGED=BlockStateProperties.WATERLOGGED;
    public static final EnumProperty<SideChainPart> SIDE_CHAIN_PART=EnumProperty.create("side_chain",SideChainPart.class);
    private final boolean flammable;
    private static final VoxelShape NORTH=Shapes.or(box(0,0,13,16,16,16),box(0,0,11,16,4,13),box(0,12,11,16,16,13));
    private static final VoxelShape SOUTH=Shapes.or(box(0,0,0,16,16,3),box(0,0,3,16,4,5),box(0,12,3,16,16,5));
    private static final VoxelShape EAST=Shapes.or(box(0,0,0,3,16,16),box(3,0,0,5,4,16),box(3,12,0,5,16,16));
    private static final VoxelShape WEST=Shapes.or(box(13,0,0,16,16,16),box(11,0,0,13,4,16),box(11,12,0,13,16,16));
    public ShelfBlock(BlockBehaviour.Properties properties,boolean flammable){super(properties);this.flammable=flammable;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(POWERED,false).setValue(WATERLOGGED,false).setValue(SIDE_CHAIN_PART,SideChainPart.UNCONNECTED));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,POWERED,WATERLOGGED,SIDE_CHAIN_PART);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ShelfBlockEntity(pos,state);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return switch(state.getValue(FACING)){case SOUTH->SOUTH;case EAST->EAST;case WEST->WEST;default->NORTH;};}
    @Override public boolean useShapeForLightOcclusion(BlockState state){return true;}
    @Override public boolean isPathfindable(BlockState state,BlockGetter level,BlockPos pos,PathComputationType type){return type==PathComputationType.WATER&&state.getFluidState().is(FluidTags.WATER);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(POWERED,context.getLevel().hasNeighborSignal(context.getClickedPos())).setValue(WATERLOGGED,context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(FACING)));}
    @Override public FluidState getFluidState(BlockState state){return state.getValue(WATERLOGGED)?Fluids.WATER.getSource(false):super.getFluidState(state);}
    @Override public BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){if(state.getValue(WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,Fluids.WATER.getTickDelay(level));return super.updateShape(state,direction,neighbor,level,pos,neighborPos);}
    @Override public int getFlammability(BlockState state,BlockGetter level,BlockPos pos,Direction face){return flammable?20:0;}
    @Override public int getFireSpreadSpeed(BlockState state,BlockGetter level,BlockPos pos,Direction face){return flammable?5:0;}
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState oldState,boolean moved){
        if(!level.isClientSide){if(state.getValue(POWERED))updateSelfAndNeighborsOnPoweringUp(level,pos,state,oldState);else updateNeighborsAfterPoweringDown(level,pos,state);}
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos neighborPos,boolean moved){
        if(!level.isClientSide){boolean powered=level.hasNeighborSignal(pos);if(state.getValue(POWERED)!=powered){
            var next=state.setValue(POWERED,powered);if(!powered)next=next.setValue(SIDE_CHAIN_PART,SideChainPart.UNCONNECTED);
            level.setBlockAndUpdate(pos,next);sound(level,pos,powered?"activate":"deactivate");level.gameEvent(null,powered?GameEvent.BLOCK_ACTIVATE:GameEvent.BLOCK_DEACTIVATE,pos);
        }}
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moved){
        if(!state.is(replacement.getBlock())){
            if(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf)Containers.dropContents(level,pos,shelf);
            if(!level.isClientSide){level.updateNeighbourForOutputSignal(pos,this);updateNeighborsAfterPoweringDown(level,pos,state);}
            super.onRemove(state,level,pos,replacement,moved);
        }
    }
    public int hitSlot(BlockHitResult hit,Direction facing){
        if(hit.getDirection()!=facing)return -1;
        var local=hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        double x=switch(facing){case NORTH->1-local.x;case SOUTH->local.x;case WEST->local.z;case EAST->1-local.z;default->0;};
        return Mth.clamp(Mth.floor(x*3),0,2);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand!=InteractionHand.MAIN_HAND||!(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf))return InteractionResult.PASS;
        int slot=hitSlot(hit,state.getValue(FACING));if(slot<0)return InteractionResult.PASS;
        var held=player.getItemInHand(hand);
        if(level.isClientSide)return state.getValue(POWERED)||!held.isEmpty()||!shelf.getItem(slot).isEmpty()?InteractionResult.SUCCESS:InteractionResult.PASS;
        if(state.getValue(POWERED)){
            if(swapHotbar(level,pos,player.getInventory()))sound(level,pos,"multi_swap");
        }else{
            if(held.isEmpty()&&shelf.getItem(slot).isEmpty())return InteractionResult.PASS;
            var removed=shelf.swapItemNoUpdate(slot,held.copy());
            player.setItemInHand(hand,player.getAbilities().instabuild&&removed.isEmpty()?held:removed);
            player.getInventory().setChanged();shelf.setChanged();
            sound(level,pos,removed.isEmpty()?"place_item":held.isEmpty()?"take_item":"single_swap");
            level.gameEvent(player,GameEvent.BLOCK_CHANGE,pos);
        }
        return InteractionResult.CONSUME;
    }
    public boolean swapHotbar(Level level,BlockPos pos,Inventory inventory){
        List<BlockPos> connected=getAllBlocksConnectedTo(level,pos);boolean swapped=false;
        for(int part=0;part<connected.size();part++){
            if(!(level.getBlockEntity(connected.get(part)) instanceof ShelfBlockEntity shelf))continue;
            for(int slot=0;slot<3;slot++){
                int hotbarSlot=9-(connected.size()-part)*3+slot;
                var placed=inventory.removeItemNoUpdate(hotbarSlot);var removed=shelf.swapItemNoUpdate(slot,placed);
                inventory.setItem(hotbarSlot,removed);swapped|=!placed.isEmpty()||!removed.isEmpty();
            }
            shelf.setChanged();
        }
        if(swapped){inventory.setChanged();level.gameEvent(null,GameEvent.BLOCK_CHANGE,pos);}return swapped;
    }
    private void sound(Level level,BlockPos pos,String action){level.playSound(null,pos,ShelfContent.SOUNDS_BY_NAME.get(action).get(),SoundSource.BLOCKS,1,1);}
    @Override public SideChainPart getSideChainPart(BlockState state){return state.getValue(SIDE_CHAIN_PART);}
    @Override public BlockState setSideChainPart(BlockState state,SideChainPart part){return state.setValue(SIDE_CHAIN_PART,part);}
    @Override public Direction getFacing(BlockState state){return state.getValue(FACING);}
    @Override public boolean isConnectable(BlockState state){return state.getBlock() instanceof ShelfBlock&&state.getValue(POWERED);}
    @Override public int getMaxChainLength(){return 3;}
    @Override public boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override public int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){
        if(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf)return (shelf.getItem(0).isEmpty()?0:1)|(shelf.getItem(1).isEmpty()?0:2)|(shelf.getItem(2).isEmpty()?0:4);
        return 0;
    }
}

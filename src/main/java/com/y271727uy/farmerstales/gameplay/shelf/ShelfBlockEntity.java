package com.y271727uy.farmerstales.gameplay.shelf;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public final class ShelfBlockEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    private boolean alignItemsToBottom;
    public ShelfBlockEntity(BlockPos pos,BlockState state){super(ShelfContent.SHELF.get(),pos,state);}
    @Override public void load(CompoundTag tag){super.load(tag);items.clear();ContainerHelper.loadAllItems(tag,items);alignItemsToBottom=tag.getBoolean("align_items_to_bottom");}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items,true);tag.putBoolean("align_items_to_bottom",alignItemsToBottom);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public int getContainerSize(){return 3;}
    @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int slot){return items.get(slot);}
    @Override public ItemStack removeItem(int slot,int count){var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())setChanged();return result;}
    @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
    @Override public void setItem(int slot,ItemStack stack){setItemNoUpdate(slot,stack);setChanged();}
    private void setItemNoUpdate(int slot,ItemStack stack){items.set(slot,stack);if(stack.getCount()>Math.min(getMaxStackSize(),stack.getMaxStackSize()))stack.setCount(Math.min(getMaxStackSize(),stack.getMaxStackSize()));}
    public ItemStack swapItemNoUpdate(int slot,ItemStack stack){var removed=removeItemNoUpdate(slot);setItemNoUpdate(slot,stack);return removed;}
    @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+0.5,worldPosition.getY()+0.5,worldPosition.getZ()+0.5)<=64;}
    @Override public void clearContent(){items.clear();setChanged();}
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
    }
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> inventoryCapability=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));
    @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability,@javax.annotation.Nullable Direction side){
        if(!isRemoved()&&capability==net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)return inventoryCapability.cast();
        return super.getCapability(capability,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();inventoryCapability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();inventoryCapability=net.minecraftforge.common.util.LazyOptional.of(()->new net.minecraftforge.items.wrapper.InvWrapper(this));}
    public boolean alignItemsToBottom(){return alignItemsToBottom;}
}

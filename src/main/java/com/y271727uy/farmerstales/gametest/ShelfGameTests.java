package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.gameplay.shelf.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("farmerstales") @PrefixGameTestTemplate(false)
public final class ShelfGameTests {
    private static ShelfBlock block(String wood){return ShelfContent.SHELVES.get(wood).get();}
    private static ShelfBlockEntity shelf(GameTestHelper h,BlockPos pos,String wood,Direction facing,boolean powered){
        h.getLevel().setBlockAndUpdate(pos,block(wood).defaultBlockState().setValue(ShelfBlock.FACING,facing).setValue(ShelfBlock.POWERED,powered));return (ShelfBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    private static BlockHitResult hit(BlockPos pos,Direction facing,int slot){
        double x=facing==Direction.NORTH?1-(slot+0.5)/3:(slot+0.5)/3;
        double z=facing==Direction.EAST?1-(slot+0.5)/3:(slot+0.5)/3;
        var point=facing.getAxis()==Direction.Axis.Z?new Vec3(pos.getX()+x,pos.getY()+0.5,pos.getZ()+0.75):new Vec3(pos.getX()+0.75,pos.getY()+0.5,pos.getZ()+z);
        return new BlockHitResult(point,facing,pos,false);
    }
    @GameTest(template="forest_empty") public static void allThirteenWoodsCraftAndKeepTheirMaterialProperties(GameTestHelper h){
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}};
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);var level=h.getLevel();
        h.assertTrue(ShelfContent.SHELVES.size()==13,"All thirteen wood types must have shelves");
        for(var entry:ShelfContent.SHELVES.entrySet()){
            var name=entry.getKey();var b=entry.getValue().get();var recipe=(CraftingRecipe)level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("farmerstales",name+"_shelf")).orElseThrow();
            var material=recipe.getIngredients().get(0).getItems()[0];grid.clearContent();for(int i:new int[]{0,1,2,6,7,8})grid.setItem(i,material.copy());
            h.assertTrue(recipe.matches(grid,level),"Six stripped logs/stems must craft "+name+" shelf");var result=recipe.assemble(grid,level.registryAccess());h.assertTrue(result.is(b.asItem())&&result.getCount()==6,"Shelf recipe must return six of the correct wood");
            var pos=h.absolutePos(new BlockPos(4,3,4));boolean nether=name.equals("crimson")||name.equals("warped");
            h.assertTrue(b.getFlammability(b.defaultBlockState(),level,pos,Direction.UP)==(nether?0:20),"Nether shelves must be fire resistant");
            h.assertTrue(b.asItem().getBurnTime(new ItemStack(b),RecipeType.SMELTING)==(nether?0:300),"Shelf fuel must respect its wood type");
            h.assertTrue(b.defaultBlockState().setValue(ShelfBlock.WATERLOGGED,true).getFluidState().is(Fluids.WATER),"Shelves must support waterlogging");
        }h.succeed();
    }
    @GameTest(template="forest_empty") public static void directSlotSwapsKeepFullStacksNbtAndCreativeRules(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer();player.getAbilities().instabuild=false;var pos=h.absolutePos(new BlockPos(10,4,10));
        for(Direction facing:Direction.Plane.HORIZONTAL){
            level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());var shelf=shelf(h,pos,"oak",facing,false);var state=shelf.getBlockState();
            for(int slot=0;slot<3;slot++){
                var apples=new ItemStack(Items.APPLE,17+slot);apples.getOrCreateTag().putString("shelf_test","slot-"+slot);player.setItemInHand(InteractionHand.MAIN_HAND,apples);
                state.use(level,player,InteractionHand.MAIN_HAND,hit(pos,facing,slot));h.assertTrue(player.getMainHandItem().isEmpty()&&shelf.getItem(slot).getCount()==17+slot,"Front hit must insert full stack into its slot for "+facing);
                player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,31));state.use(level,player,InteractionHand.MAIN_HAND,hit(pos,facing,slot));
                h.assertTrue(player.getMainHandItem().getCount()==17+slot&&player.getMainHandItem().getTag().getString("shelf_test").equals("slot-"+slot)&&shelf.getItem(slot).getCount()==31,"Swapping must preserve NBT and counts");
                player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);state.use(level,player,InteractionHand.MAIN_HAND,hit(pos,facing,slot));h.assertTrue(player.getMainHandItem().is(Items.WHEAT)&&shelf.getItem(slot).isEmpty(),"Empty hand must retrieve stack");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,5));state.use(level,player,InteractionHand.MAIN_HAND,hit(pos,facing.getOpposite(),0));h.assertTrue(shelf.isEmpty(),"Back face must not insert");
            player.getAbilities().instabuild=true;state.use(level,player,InteractionHand.MAIN_HAND,hit(pos,facing,0));h.assertTrue(player.getMainHandItem().getCount()==5&&shelf.getItem(0).getCount()==5,"Creative placement must retain held stack");
            player.getAbilities().instabuild=false;shelf.clearContent();
        }h.succeed();
    }
    @GameTest(template="forest_empty") public static void inventorySavesSyncsAndSupportsAutomationWithoutDuplicatingDrops(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,4,10));var shelf=shelf(h,pos,"poplar",Direction.NORTH,false);
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
        var stack=new ItemStack(Items.DIAMOND,17);stack.getOrCreateTag().putString("shelf_test","persistent");shelf.setItem(1,stack);
        var saved=shelf.saveWithFullMetadata();saved.putBoolean("align_items_to_bottom",true);var restored=new ShelfBlockEntity(pos,shelf.getBlockState());restored.load(saved);
        h.assertTrue(ItemStack.matches(restored.getItem(1),stack)&&restored.alignItemsToBottom(),"Save/load must keep counts, NBT and display alignment");
        var clientCopy=new ShelfBlockEntity(pos,shelf.getBlockState());clientCopy.load(shelf.getUpdateTag());h.assertTrue(ItemStack.matches(clientCopy.getItem(1),stack)&&shelf.getUpdatePacket()!=null,"Client update must contain inventory");
        var handler=shelf.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing inventory capability"));
        h.assertTrue(handler.insertItem(0,new ItemStack(Items.WHEAT,23),false).isEmpty(),"Pipes must be able to insert");h.assertTrue(handler.extractItem(0,3,false).getCount()==3&&shelf.getItem(0).getCount()==20,"Pipes must extract the requested amount");
        var state=shelf.getBlockState();level.setBlockAndUpdate(pos,state.setValue(ShelfBlock.POWERED,true));h.assertTrue(level.getBlockEntity(pos)==shelf&&shelf.getItem(1).getCount()==17,"Changing power must preserve the block entity");
        level.destroyBlock(pos,true);var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
        h.assertTrue(drops.stream().map(ItemEntity::getItem).filter(i->i.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum()==17,"Breaking must drop stored diamonds exactly once");
        h.assertTrue(drops.stream().map(ItemEntity::getItem).filter(i->i.is(Items.WHEAT)).mapToInt(ItemStack::getCount).sum()==20,"Breaking must drop stored wheat exactly once");
        h.assertTrue(drops.stream().map(ItemEntity::getItem).filter(i->i.is(block("poplar").asItem())).mapToInt(ItemStack::getCount).sum()==1,"Breaking must also drop shelf itself");h.succeed();
    }
    @GameTest(template="forest_empty") public static void poweredShelvesJoinAtMostThreeAndSwapCorrectHotbarRange(GameTestHelper h){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(10,5,10));var player=h.makeMockPlayer();
        for(int x=0;x<4;x++){level.setBlockAndUpdate(base.east(x).below(),Blocks.REDSTONE_BLOCK.defaultBlockState());shelf(h,base.east(x),x%2==0?"oak":"pale_oak",Direction.NORTH,true);}
        var connected=block("oak").getAllBlocksConnectedTo(level,base);h.assertTrue(connected.size()==3&&block("oak").getAllBlocksConnectedTo(level,base.east(3)).size()==1,"A fourth powered shelf must stay outside a full chain");
        for(int i=0;i<9;i++)player.getInventory().setItem(i,new ItemStack(Items.WHEAT,i+1));
        for(int part=0;part<3;part++)for(int slot=0;slot<3;slot++)((ShelfBlockEntity)level.getBlockEntity(connected.get(part))).setItem(slot,new ItemStack(Items.APPLE,part*3+slot+11));
        block("oak").swapHotbar(level,base,player.getInventory());
        for(int i=0;i<9;i++){h.assertTrue(player.getInventory().getItem(i).is(Items.APPLE)&&player.getInventory().getItem(i).getCount()==i+11,"Three connected shelves must swap the entire hotbar in visual order");var be=(ShelfBlockEntity)level.getBlockEntity(connected.get(i/3));h.assertTrue(be.getItem(i%3).is(Items.WHEAT)&&be.getItem(i%3).getCount()==i+1,"Hotbar stack must move to matching shelf slot");}
        level.setBlockAndUpdate(base.east(1).below(),Blocks.STONE.defaultBlockState());
        h.assertTrue(block("oak").getAllBlocksConnectedTo(level,base).size()==1,"Powering down the middle shelf must split the chain");
        player.getInventory().clearContent();player.getInventory().setItem(0,new ItemStack(Items.STICK,7));
        block("oak").swapHotbar(level,base,player.getInventory());h.assertTrue(player.getInventory().getItem(0).is(Items.STICK)&&player.getInventory().getItem(6).is(Items.WHEAT),"A single shelf must swap only hotbar slots 6 through 8");h.succeed();
    }
    @GameTest(template="forest_empty") public static void comparatorReadsSlotBitsOnlyBehindTheShelf(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,5,10));var shelf=shelf(h,pos,"birch",Direction.NORTH,false);shelf.setItem(0,new ItemStack(Items.STICK));shelf.setItem(2,new ItemStack(Items.APPLE));
        for(Direction dir:Direction.Plane.HORIZONTAL){var comparator=pos.relative(dir);level.setBlockAndUpdate(comparator.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(comparator,Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING,dir.getOpposite()));}
        h.runAfterDelay(6,()->{
            for(Direction dir:Direction.Plane.HORIZONTAL){var comparator=pos.relative(dir);var be=(net.minecraft.world.level.block.entity.ComparatorBlockEntity)level.getBlockEntity(comparator);h.assertTrue(be.getOutputSignal()==(dir==Direction.SOUTH?5:0),"Comparator output must be rear-only bitmask 1|4; side="+dir+" value="+be.getOutputSignal());}
            h.succeed();
        });
    }
}

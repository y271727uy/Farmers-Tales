package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class ForestBuildingGameTests {
    @GameTest(template="forest_empty")
    public static void bothWoodSetsCraftAllBuildingParts(GameTestHelper helper) {
        var level=helper.getLevel();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int index){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        String[] parts={"stairs","slab","fence","fence_gate","door","trapdoor","button","pressure_plate"};
        String[][] patterns={{"P  ","PP ","PPP"},{"PPP","   ","   "},{"PSP","PSP","   "},{"SPS","SPS","   "},{"PP ","PP ","PP "},{"PPP","PPP","   "},{"P  ","   ","   "},{"PP ","   ","   "}};
        int[] counts={4,6,3,1,3,2,1,1};
        for(String tree:new String[]{"pale_oak","poplar"})for(int type=0;type<parts.length;type++) {
            grid.clearContent();var planks=ForestBlocks.ALL.get(tree+"_planks").get();
            for(int y=0;y<3;y++)for(int x=0;x<3;x++) {
                char material=patterns[type][y].charAt(x);
                if(material!=' ')grid.setItem(y*3+x,new ItemStack(material=='P'?planks.asItem():Items.STICK));
            }
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);
            helper.assertTrue(recipe.isPresent(),"Missing recipe for "+tree+"_"+parts[type]);
            var result=recipe.get().assemble(grid,level.registryAccess());
            helper.assertTrue(result.is(ForestBlocks.BUILDING.get(tree+"_"+parts[type]).get().asItem()) && result.getCount()==counts[type],"Building recipe must produce its correct wood and count");
        }
        helper.succeed();
    }
    private static void place(GameTestHelper helper,BlockPos pos,Block block) {
        var player=helper.makeMockPlayer();player.moveTo(Vec3.atBottomCenterOf(pos.south(2)));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()),Direction.UP,pos.below(),false);
        var result=((BlockItem)block.asItem()).place(new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)));
        helper.assertTrue(result.consumesAction(),"Block item must place "+block);
    }
    @GameTest(template="forest_empty")
    public static void woodDoorsSlabsAndButtonsKeepVanillaBehavior(GameTestHelper helper) {
        var level=helper.getLevel();
        for(int set=0;set<2;set++) {
            String tree=set==0?"pale_oak":"poplar";var pos=helper.absolutePos(new BlockPos(5+set*12,2,5));
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            var door=(DoorBlock)ForestBlocks.BUILDING.get(tree+"_door").get();place(helper,pos,door);
            helper.assertTrue(level.getBlockState(pos.above()).is(door) && level.getBlockState(pos.above()).getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER,"Door item must place both halves");
            var player=helper.makeMockPlayer();level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false));
            helper.assertTrue(level.getBlockState(pos).getValue(DoorBlock.OPEN) && level.getBlockState(pos.above()).getValue(DoorBlock.OPEN),"Wooden door must open both halves by hand");
            int lower=Block.getDrops(level.getBlockState(pos),level,pos,null,null,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum();
            int upper=Block.getDrops(level.getBlockState(pos.above()),level,pos.above(),null,null,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(lower==1 && upper==0,"Two door halves must drop exactly one item");
            var slab=ForestBlocks.BUILDING.get(tree+"_slab").get();
            var drops=Block.getDrops(slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),level,pos,null,null,ItemStack.EMPTY);
            helper.assertTrue(drops.size()==1 && drops.get(0).getCount()==2,"Double slab must drop two slabs");
            var button=(ButtonBlock)ForestBlocks.BUILDING.get(tree+"_button").get();var buttonPos=pos.east(3);
            level.setBlockAndUpdate(buttonPos.below(),Blocks.STONE.defaultBlockState());place(helper,buttonPos,button);
            button.press(level.getBlockState(buttonPos),level,buttonPos);
            helper.assertTrue(level.getBlockState(buttonPos).getValue(ButtonBlock.POWERED),"Wood button must emit redstone when pressed");
            button.tick(level.getBlockState(buttonPos),level,buttonPos,level.random);
            helper.assertTrue(!level.getBlockState(buttonPos).getValue(ButtonBlock.POWERED),"Wood button must release after its scheduled tick");
        }
        helper.succeed();
    }
}

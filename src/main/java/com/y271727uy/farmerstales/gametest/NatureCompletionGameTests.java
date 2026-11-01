package com.y271727uy.farmerstales.gametest;
import com.y271727uy.farmerstales.all.*;
import com.y271727uy.farmerstales.gameplay.environment.forest.*;
import com.y271727uy.farmerstales.variant.WolfSoundHolder;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
@GameTestHolder("farmerstales") @PrefixGameTestTemplate(false)
public final class NatureCompletionGameTests {
    @GameTest(template="forest_empty") public static void mossClimbsWithBonemealAndUpperSidesDropNothing(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,2,10));var moss=(PaleMossCarpet)ForestBlocks.PALE_MOSS_CARPET.get();
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        for(int y=0;y<2;y++)level.setBlockAndUpdate(pos.north().above(y),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,moss.defaultBlockState());
        h.assertTrue(level.getBlockState(pos).getValue(PaleMossCarpet.SIDES.get(Direction.NORTH))==WallSide.LOW,"Ground moss must attach to a neighboring wall");
        h.assertTrue(moss.isValidBonemealTarget(level,pos,level.getBlockState(pos),false),"Wall must allow bonemeal growth");
        moss.performBonemeal(level,level.random,pos,level.getBlockState(pos));
        var upper=level.getBlockState(pos.above());h.assertTrue(upper.is(moss)&&!upper.getValue(PaleMossCarpet.BASE),"Bonemeal must grow wall-only upper moss");
        h.assertTrue(level.getBlockState(pos).getValue(PaleMossCarpet.SIDES.get(Direction.NORTH))==WallSide.TALL,"Lower moss must join the upper wall face");
        h.assertTrue(Block.getDrops(upper,level,pos.above(),null,null,ItemStack.EMPTY).isEmpty(),"Upper moss must not duplicate carpet drops");
        h.assertTrue(Block.getDrops(level.getBlockState(pos),level,pos,null,null,ItemStack.EMPTY).stream().mapToInt(ItemStack::getCount).sum()==1,"Base moss must drop exactly one carpet");
        level.destroyBlock(pos,false);h.assertTrue(level.getBlockState(pos.above()).isAir(),"Removing the base must remove the upper moss");
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void signTypesRetainTwoSidedTextGlowAndWaxAcrossSave(GameTestHelper h){
        var level=h.getLevel();int index=0;
        for(var entry:ForestTravel.BLOCKS_BY_NAME.entrySet()){
            var pos=h.absolutePos(new BlockPos(3+index*3,3,5));index++;
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
            var block=entry.getValue().get();level.setBlock(pos,block.defaultBlockState(),2);
            var be=(SignBlockEntity)level.getBlockEntity(pos);boolean hanging=entry.getKey().contains("hanging");
            h.assertTrue(be!=null&&be.getType()==(hanging?ForestTravel.HANGING_SIGN.get():ForestTravel.SIGN.get()),"Both wall and standing forms must create the custom sign block entity");
            be.setText(be.getFrontText().setMessage(0,Component.literal("front")).setHasGlowingText(true),true);
            be.setText(be.getBackText().setMessage(0,Component.literal("back")),false);be.setWaxed(true);
            var saved=be.saveWithFullMetadata();
            var restored=(SignBlockEntity)BlockEntity.loadStatic(pos,block.defaultBlockState(),saved);
            h.assertTrue(restored!=null&&restored.getFrontText().getMessage(0,false).getString().equals("front")&&restored.getBackText().getMessage(0,false).getString().equals("back"),"Both text faces must survive persistence");
            h.assertTrue(restored.isWaxed()&&restored.getFrontText().hasGlowingText(),"Wax and glow must persist");
            h.assertTrue(((EntityBlock)block).getTicker(level,block.defaultBlockState(),be.getType())!=null,"Custom sign must retain its server ticker");
            var drops=Block.getDrops(block.defaultBlockState(),level,pos,be,null,ItemStack.EMPTY);
            String item=entry.getKey().replace("_wall","");
            h.assertTrue(drops.size()==1&&drops.get(0).is(ForestTravel.ITEMS_BY_NAME.get(item).get()),"All mounted forms must drop their corresponding sign item");
        }
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void boatsPersistWoodAndChestContentsAndDropOwnItems(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,3,10));
        for(String wood:new String[]{"pale_oak","poplar"})for(boolean chest:new boolean[]{false,true}){
            var boat=ForestTravel.createBoat(level,wood,chest,Vec3.atCenterOf(pos));
            if(chest)((ChestBoat)boat).setItem(3,new ItemStack(Items.DIAMOND,7));
            var tag=new CompoundTag();boat.save(tag);var loaded=EntityType.loadEntityRecursive(tag,level,e->e);
            h.assertTrue(loaded instanceof ForestBoatWood timber&&timber.wood().equals(wood),"Entity type must retain the boat's wood after save/load");
            String name=wood+(chest?"_chest_boat":"_boat");
            h.assertTrue(((Boat)loaded).getDropItem()==ForestTravel.ITEMS_BY_NAME.get(name).get(),"Boat must drop its own registered item");
            if(chest)h.assertTrue(((ChestBoat)loaded).getItem(3).getCount()==7,"Chest boat inventory must survive save/load");
            boat.discard();loaded.discard();
        }
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void dispensersLaunchBothBoatFamiliesAndFallbackOnLand(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,3,10));
        level.setBlockAndUpdate(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.EAST));
        var be=(DispenserBlockEntity)level.getBlockEntity(pos);
        for(String wood:new String[]{"pale_oak","poplar"})for(boolean chest:new boolean[]{false,true}){
            var item=ForestTravel.ITEMS_BY_NAME.get(wood+(chest?"_chest_boat":"_boat")).get();
            level.setBlockAndUpdate(pos.east(),Blocks.WATER.defaultBlockState());be.setItem(0,new ItemStack(item));
            Blocks.DISPENSER.tick(level.getBlockState(pos),level,pos,level.random);
            var boats=level.getEntitiesOfClass(Boat.class,new AABB(pos).inflate(5));
            h.assertTrue(boats.stream().anyMatch(b->b.getDropItem()==item),"Dispenser must spawn the correct custom boat");boats.forEach(Entity::discard);
            level.setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());be.setItem(0,new ItemStack(item));
            Blocks.DISPENSER.tick(level.getBlockState(pos),level,pos,level.random);
            h.assertTrue(level.getEntitiesOfClass(Boat.class,new AABB(pos).inflate(5)).isEmpty(),"Blocked dispenser must drop an item rather than spawn a boat");
        }
        h.succeed();
    }
    private static final class AudibleWolf extends Wolf {
        AudibleWolf(Level level){super(EntityType.WOLF,level);}
        SoundEvent ambient(){return getAmbientSound();} SoundEvent death(){return getDeathSound();}
    }
    @GameTest(template="crop_stand_empty") public static void wolfVoicesPersistIndependentlyAndReplaceActualCalls(GameTestHelper h){
        String[] names={"classic","angry","big","cute","grumpy","puglin","sad"};
        for(int voice=0;voice<7;voice++){
            var wolf=new AudibleWolf(h.getLevel());((WolfSoundHolder)(Object)wolf).farmerstales$setSoundVariant(voice);
            var nbt=wolf.saveWithoutId(new CompoundTag());var copy=new AudibleWolf(h.getLevel());copy.readAdditionalSaveData(nbt);
            h.assertTrue(((WolfSoundHolder)(Object)copy).farmerstales$getSoundVariant()==voice,"Wolf voice must survive persistence");
            var actual=ForgeRegistries.SOUND_EVENTS.getKey(copy.death());
            h.assertTrue(actual.toString().equals(voice==0?"minecraft:entity.wolf.death":"farmerstales:entity.wolf_"+names[voice]+".death"),"Actual wolf death sound must select the saved voice");
            h.assertTrue(copy.ambient()!=null,"Every voice must have a usable ambient call");wolf.discard();copy.discard();
        }
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void desertAmbienceRequiresDeepDryGroundAndOpenSurroundings(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(16,3,16));
        level.setBlockAndUpdate(pos,Blocks.SAND.defaultBlockState());
        for(var direction:Direction.Plane.HORIZONTAL){var side=pos.relative(direction,8);level.setBlockAndUpdate(side,Blocks.SAND.defaultBlockState());}
        h.assertTrue(NatureAmbience.sandSurroundings(level,pos),"Open dry terrain must qualify for ambient sand sounds");
        level.setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());h.assertTrue(!NatureAmbience.sandSurroundings(level,pos),"Covered sand must stay silent");
        level.setBlockAndUpdate(pos.below(),Blocks.SAND.defaultBlockState());
        h.assertTrue(NatureAmbience.dryGrassGround(level,pos.above()),"Dry grass above two dry substrate blocks must qualify");
        level.setBlockAndUpdate(pos.below(),Blocks.DIRT.defaultBlockState());h.assertTrue(!NatureAmbience.dryGrassGround(level,pos.above()),"A single thin sand layer must not trigger dry-grass sound");
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void travelItemsCraftAndBoatItemsLaunchFromPlayerUse(GameTestHelper h){
        var level=h.getLevel();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot){return ItemStack.EMPTY;}
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return true;}
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(String wood:new String[]{"pale_oak","poplar"})for(String part:new String[]{"sign","hanging_sign","boat","chest_boat"}){
            grid.clearContent();var planks=ForestBlocks.ALL.get(wood+"_planks").get().asItem();
            if(part.equals("sign")){for(int slot=0;slot<6;slot++)grid.setItem(slot,new ItemStack(planks));grid.setItem(7,new ItemStack(Items.STICK));}
            else if(part.equals("hanging_sign")){grid.setItem(0,new ItemStack(Items.CHAIN));grid.setItem(2,new ItemStack(Items.CHAIN));for(int slot=3;slot<9;slot++)grid.setItem(slot,new ItemStack(ForestBlocks.ALL.get("stripped_"+wood+"_log").get()));}
            else if(part.equals("boat")){for(int slot:new int[]{0,2,3,4,5})grid.setItem(slot,new ItemStack(planks));}
            else{grid.setItem(0,new ItemStack(ForestTravel.ITEMS_BY_NAME.get(wood+"_boat").get()));grid.setItem(1,new ItemStack(Items.CHEST));}
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);
            h.assertTrue(recipe.isPresent()&&recipe.get().assemble(grid,level.registryAccess()).is(ForestTravel.ITEMS_BY_NAME.get(wood+"_"+part).get()),"Travel item must craft from its own wood: "+wood+"_"+part);
        }
        for(int x=3;x<14;x++)for(int z=3;z<14;z++)h.setBlock(new BlockPos(x,2,z),Blocks.WATER);
        var player=h.makeMockPlayer();var position=h.absolutePos(new BlockPos(8,3,3));player.moveTo(Vec3.atBottomCenterOf(position));player.setXRot(45);player.setYRot(0);player.setYHeadRot(0);
        for(String wood:new String[]{"pale_oak","poplar"})for(boolean chest:new boolean[]{false,true}){
            var item=ForestTravel.ITEMS_BY_NAME.get(wood+(chest?"_chest_boat":"_boat")).get();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(item));
            var result=item.use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
            h.assertTrue(result.getResult().consumesAction(),"Player right-click on water must launch the custom boat");
            var boats=level.getEntitiesOfClass(Boat.class,new AABB(position).inflate(8));
            h.assertTrue(boats.stream().anyMatch(b->b.getDropItem()==item),"Player use must launch the correct wood and chest variant");boats.forEach(Entity::discard);
        }
        h.succeed();
    }

    @GameTest(template="forest_empty") public static void fallenBoatsDropTheirPlanksAndKeepStickDrops(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(15,2,15));
        for(int x=10;x<21;x++)for(int z=10;z<21;z++)h.setBlock(new BlockPos(x,1,z),Blocks.STONE);
        for(String wood:new String[]{"pale_oak","poplar"}){
            var boat=ForestTravel.createBoat(level,wood,false,Vec3.atBottomCenterOf(pos));level.addFreshEntity(boat);
            // Establish vanilla ON_LAND status before exercising its accumulated-fall branch.
            for(int tick=0;tick<5;tick++)boat.tick();
            boat.fallDistance=5;boat.setDeltaMovement(0,-0.1,0);boat.tick();
            h.assertTrue(boat.isRemoved(),"Boat landing with accumulated fall distance must break");
            var items=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(pos).inflate(6));
            int planks=items.stream().filter(e->e.getItem().is(ForestBlocks.ALL.get(wood+"_planks").get().asItem())).mapToInt(e->e.getItem().getCount()).sum();
            int sticks=items.stream().filter(e->e.getItem().is(Items.STICK)).mapToInt(e->e.getItem().getCount()).sum();
            h.assertTrue(planks==3&&sticks==2,"Broken boat must drop three matching planks and two unchanged sticks");items.forEach(Entity::discard);
        }
        h.succeed();
    }

}

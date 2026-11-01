package com.y271727uy.farmerstales.gametest;
import com.y271727uy.farmerstales.gameplay.building.*;
import com.y271727uy.farmerstales.variant.AgeLockHolder;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
@GameTestHolder("farmerstales") @PrefixGameTestTemplate(false)
public final class BuildingGameTests {
    private static PlayerInteractEvent.EntityInteract use(net.minecraft.world.entity.player.Player player,Entity mob){
        var event=new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,mob);MinecraftForge.EVENT_BUS.post(event);return event;
    }
    @GameTest(template="forest_empty") public static void tuffRecipesStonecuttingAndSlabLoot(GameTestHelper h){
        var level=h.getLevel();var manager=level.getRecipeManager();var inv=new SimpleContainer(1);inv.setItem(0,new ItemStack(Blocks.TUFF));
        h.assertTrue(BuildingContent.TUFF.size()==13,"All thirteen new building blocks must register");
        for(String name:BuildingContent.TUFF.keySet()){
            h.assertTrue(manager.byKey(ResourceLocation.fromNamespaceAndPath("farmerstales",name)).isPresent(),"Missing crafting recipe: "+name);
            String from=name+"_from_tuff_stonecutting";
            var cutter=(StonecutterRecipe)manager.byKey(ResourceLocation.fromNamespaceAndPath("farmerstales",from)).orElseThrow();
            h.assertTrue(cutter.matches(inv,level),"Raw tuff must cut directly to "+name);
            var result=cutter.assemble(inv,level.registryAccess());
            h.assertTrue(result.is(BuildingContent.TUFF.get(name).get().asItem())&&result.getCount()==(name.endsWith("_slab")?2:1),"Incorrect stonecutter output: "+name);
            if(name.endsWith("_slab")){
                var block=BuildingContent.TUFF.get(name).get();var pos=h.absolutePos(new BlockPos(4,2,4));
                var state=block.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE);level.setBlockAndUpdate(pos,state);
                var drops=Block.getDrops(state,level,pos,null,null,new ItemStack(Items.IRON_PICKAXE));
                h.assertTrue(drops.size()==1&&drops.get(0).is(block.asItem())&&drops.get(0).getCount()==2,"Double slab must drop two: "+name);
            }
        }
        h.succeed();
    }
    @GameTest(template="forest_empty") public static void goldenDandelionLocksSavesAndResumesBabyGrowth(GameTestHelper h){
        var level=h.getLevel();var cow=EntityType.COW.create(level);cow.setAge(-100);var lock=(AgeLockHolder)cow;
        var player=h.makeMockPlayer();player.getAbilities().instabuild=false;player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuildingContent.GOLDEN_DANDELION.get(),3));
        h.assertTrue(use(player,cow).isCanceled()&&lock.farmerstales$isAgeLocked(),"Flower must lock baby growth");
        h.assertTrue(player.getMainHandItem().getCount()==2&&cow.getAge()==-24000&&cow.isPersistenceRequired(),"Lock must consume one flower, reset baby age and persist mob");
        use(player,cow);h.assertTrue(lock.farmerstales$isAgeLocked()&&player.getMainHandItem().getCount()==2,"Cooldown must prevent immediate second use");
        for(int i=0;i<40;i++)cow.aiStep();h.assertTrue(cow.getAge()==-24000,"Locked baby must not age naturally");
        cow.ageUp(1000,true);h.assertTrue(cow.getAge()==-24000,"Feeding/API growth must not age locked babies");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.WHEAT,2));cow.mobInteract(player,InteractionHand.MAIN_HAND);h.assertTrue(player.getMainHandItem().getCount()==2,"Locked baby must not waste growth food");
        var saved=cow.saveWithoutId(new CompoundTag());var restored=EntityType.COW.create(level);restored.readAdditionalSaveData(saved);
        h.assertTrue(((AgeLockHolder)restored).farmerstales$isAgeLocked()&&restored.getAge()==-24000,"Age lock must survive save and load");restored.aiStep();h.assertTrue(restored.getAge()==-24000,"Loaded baby must remain paused");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuildingContent.GOLDEN_DANDELION.get(),2));use(player,cow);
        h.assertTrue(!lock.farmerstales$isAgeLocked()&&player.getMainHandItem().getCount()==1,"Second flower must resume growth");cow.aiStep();h.assertTrue(cow.getAge()==-23999,"Unlocked baby must age normally");cow.ageUp(1,true);h.assertTrue(cow.getAge()==-23979,"Feeding growth must resume too");
        var wolf=EntityType.WOLF.create(level);wolf.setTame(true);wolf.setBaby(true);wolf.setHealth(1);
        ((AgeLockHolder)wolf).farmerstales$setAgeLocked(true);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.PORKCHOP));wolf.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(wolf.getHealth()>1&&wolf.isBaby(),"Age locking must preserve food healing for injured wolves");
        cow.discard();restored.discard();wolf.discard();h.succeed();
    }
    @GameTest(template="forest_empty") public static void goldenDandelionCraftPotAndInteractionRestrictions(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuildingContent.GOLDEN_DANDELION.get(),2));
        var adult=EntityType.COW.create(level);h.assertTrue(!use(player,adult).isCanceled(),"Adults must refuse flower");
        var villager=EntityType.VILLAGER.create(level);villager.setBaby(true);h.assertTrue(!use(player,villager).isCanceled(),"Baby villagers must not be age locked");
        var baby=EntityType.COW.create(level);baby.setBaby(true);player.getAbilities().instabuild=true;use(player,baby);h.assertTrue(player.getMainHandItem().getCount()==2,"Creative mode must not consume flowers");
        var bee=EntityType.BEE.create(level);h.assertTrue(!bee.isFood(new ItemStack(BuildingContent.GOLDEN_DANDELION.get())),"Golden flower must not be bee breeding food");
        var pos=h.absolutePos(new BlockPos(4,2,4));level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.FLOWER_POT.defaultBlockState());
        level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(level.getBlockState(pos).is(BuildingContent.POTTED_GOLDEN_DANDELION.get()),"Golden flower must enter flower pots");
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null,0){public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int i){return ItemStack.EMPTY;}public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}},3,3);
        for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(i==4?Items.DANDELION:Items.GOLD_NUGGET));
        var recipe=level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("farmerstales","golden_dandelion")).orElseThrow();
        h.assertTrue(((CraftingRecipe)recipe).matches(grid,level),"Eight nuggets around a dandelion must craft the golden flower");
        grid.clearContent();grid.setItem(0,new ItemStack(BuildingContent.GOLDEN_DANDELION.get()));
        var dye=(CraftingRecipe)level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("farmerstales","yellow_dye_from_golden_dandelion")).orElseThrow();
        h.assertTrue(dye.matches(grid,level)&&dye.assemble(grid,level.registryAccess()).is(Items.YELLOW_DYE),"Golden flower must craft yellow dye");
        grid.setItem(1,new ItemStack(Items.BOWL));grid.setItem(2,new ItemStack(Items.RED_MUSHROOM));grid.setItem(3,new ItemStack(Items.BROWN_MUSHROOM));
        var stew=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow().assemble(grid,level.registryAccess());
        h.assertTrue(stew.is(Items.SUSPICIOUS_STEW)&&stew.hasTag(),"Vanilla suspicious stew recipe must accept golden flowers and attach saturation");
        adult.discard();villager.discard();baby.discard();bee.discard();h.succeed();
    }
}

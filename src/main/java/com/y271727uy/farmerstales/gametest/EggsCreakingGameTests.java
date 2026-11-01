package com.y271727uy.farmerstales.gametest;

import com.mojang.authlib.GameProfile;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.animal_variants.VariantEggs;
import com.y271727uy.farmerstales.gameplay.creaking.*;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import com.y271727uy.farmerstales.variant.AnimalAppearanceVariantHolder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class EggsCreakingGameTests {
    @GameTest(template="forest_empty")
    public static void chickenVariantsActuallyLayTheirOwnEggs(GameTestHelper helper) {
        var level = helper.getLevel(); long day = level.getDayTime(); level.setDayTime(6000);
        try {
            Item[] eggs = {VariantEggs.BLUE_EGG.get(), VariantEggs.BROWN_EGG.get(), Items.EGG};
            String[] variants = {"cold", "warm", "temperate"};
            for (int i=0; i<3; i++) {
                var pos = helper.absolutePos(new BlockPos(5+i*9, 3, 5));
                var chicken = EntityType.CHICKEN.create(level);
                chicken.moveTo(Vec3.atCenterOf(pos));
                ((AnimalAppearanceVariantHolder)chicken).farmerstales$setAppearanceVariant(variants[i]);
                chicken.eggTime=1; chicken.aiStep();
                var expected=eggs[i];
                var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3));
                helper.assertTrue(drops.stream().anyMatch(e->e.getItem().is(expected)), "Actual aiStep must lay the variant's egg: "+variants[i]);
                chicken.discard();
            }
        } finally { level.setDayTime(day); }
        helper.succeed();
    }

    private static final class TestEgg extends ThrownEgg {
        TestEgg(Level level, BlockPos pos, Item item, long seed) {
            super(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5);
            setItem(new ItemStack(item));random.setSeed(seed);
        }
        void impact(BlockPos pos) { onHit(new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false)); }
    }
    @GameTest(template="forest_empty")
    public static void thrownEggsHatchVariantsBeforeSpawnInitialization(GameTestHelper helper) {
        var level=helper.getLevel();
        Item[] eggs={VariantEggs.BLUE_EGG.get(),VariantEggs.BROWN_EGG.get(),Items.EGG};
        String[] variants={"cold","warm","temperate"};
        for (int type=0;type<3;type++) {
            var pos=helper.absolutePos(new BlockPos(5+type*9,3,10));
            // Exercise the actual vanilla impact method and its injection, including failed hatches.
            for (int seed=0;seed<128;seed++) new TestEgg(level,pos,eggs[type],2026L+seed*7919L).impact(pos);
            var chicks=level.getEntitiesOfClass(Chicken.class,new AABB(pos).inflate(2));
            helper.assertTrue(!chicks.isEmpty(),"Egg fixture must include successful hatches");
            for (var chick:chicks) {
                helper.assertTrue(chick.isBaby() && ((AnimalAppearanceVariantHolder)chick).farmerstales$getAppearanceVariant().equals(variants[type]),"Hatched chicken must retain egg variant even in a different biome");
                chick.discard();
            }
        }
        helper.succeed();
    }
    @GameTest(template="crop_stand_empty")
    public static void dispensersFireColoredEggProjectiles(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        for (Item item:new Item[]{VariantEggs.BLUE_EGG.get(),VariantEggs.BROWN_EGG.get()}) {
            level.setBlockAndUpdate(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.UP));
            var dispenser=(DispenserBlockEntity)level.getBlockEntity(pos);
            dispenser.setItem(0,new ItemStack(item,2));
            Blocks.DISPENSER.tick(level.getBlockState(pos),level,pos,level.random);
            var projectiles=level.getEntitiesOfClass(ThrownEgg.class,new AABB(pos).inflate(3));
            helper.assertTrue(projectiles.stream().anyMatch(e->e.getItem().is(item)),"Dispenser must throw a colored egg rather than dropping an item");
            helper.assertTrue(dispenser.getItem(0).getCount()==1,"Dispenser must consume exactly one egg");
            projectiles.forEach(Entity::discard);
        }
        helper.succeed();
    }
    private static CreakingHeartBlockEntity heart(GameTestHelper helper) {
        var level=helper.getLevel();
        for(int x=1;x<31;x++) for(int z=1;z<31;z++) helper.setBlock(new BlockPos(x,1,z),Blocks.GRASS_BLOCK);
        var pos=helper.absolutePos(new BlockPos(16,6,16));
        level.setBlockAndUpdate(pos.below(),ForestBlocks.PALE_OAK_LOG.get().defaultBlockState());
        level.setBlockAndUpdate(pos.above(),ForestBlocks.PALE_OAK_LOG.get().defaultBlockState());
        level.setBlockAndUpdate(pos,CreakingContent.HEART.get().defaultBlockState().setValue(CreakingHeartBlock.NATURAL,true));
        return (CreakingHeartBlockEntity)level.getBlockEntity(pos);
    }
    private static void tickHeart(CreakingHeartBlockEntity heart,GameTestHelper helper) {
        for(int i=0;i<20;i++) CreakingHeartBlockEntity.tick(helper.getLevel(),heart.getBlockPos(),heart.getBlockState(),heart);
    }
    @GameTest(template="forest_empty")
    public static void heartLifecycleSurvivesSerializationAndPreventsDuplicates(GameTestHelper helper) {
        var level=helper.getLevel();long day=level.getDayTime();
        var heart=heart(helper); CreakingEntity mob=null;
        try {
            level.setDayTime(6000);
            helper.assertTrue(!heart.trySpawn(level),"Daytime hearts must not spawn a protector");
            level.setDayTime(18000);
            for(int i=0;i<20 && heart.protector()==null;i++) heart.trySpawn(level);
            mob=heart.protector();helper.assertTrue(mob!=null,"Aligned night heart must spawn its protector");
            helper.assertTrue(!heart.trySpawn(level),"One heart must never spawn a second protector");
            var saved=heart.saveWithoutMetadata();
            heart.load(saved);tickHeart(heart,helper);
            helper.assertTrue(heart.protector()==mob && !heart.trySpawn(level),"Reloading UUID binding must reuse the living protector");
            var copy=CreakingContent.CREAKING.get().create(level);copy.readAdditionalSaveData(mob.saveWithoutId(new CompoundTag()));
            helper.assertTrue(heart.getBlockPos().equals(copy.heartPosition()),"Entity NBT must retain heart binding");copy.discard();
            helper.assertTrue(heart.outputSignal()>0,"Linked heart must supply comparator output");
            level.setDayTime(6000);tickHeart(heart,helper);
            helper.assertTrue(mob.isTearingDown() && heart.protectorId()==null,"Sunrise must detach and dismantle an unnamed protector");
            for(int i=0;i<47;i++) mob.tick();
            helper.assertTrue(mob.isRemoved(),"Dismantling must finish without dropping a normal mob corpse");
            helper.assertTrue(heart.outputSignal()==0,"Removed protector must reset comparator");
            level.setDayTime(18000);level.setBlockAndUpdate(heart.getBlockPos().above(),Blocks.AIR.defaultBlockState());
            helper.assertTrue(!heart.trySpawn(level),"Heart without aligned logs must remain inert");
        } finally { if(mob!=null) mob.discard(); heart.removeProtector();level.setDayTime(day); }
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void gazeFreezesAndPlayerHitsProduceResinWithCooldown(GameTestHelper helper) {
        var level=helper.getLevel();long day=level.getDayTime();var heart=heart(helper);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"creaking-test"));
        CreakingEntity mob=null;
        try {
            level.setDayTime(18000);
            for(int i=0;i<20 && heart.protector()==null;i++) heart.trySpawn(level);
            mob=heart.protector();helper.assertTrue(mob!=null,"Protector spawn fixture");
            var pos=helper.absolutePos(new BlockPos(16,2,10));mob.moveTo(Vec3.atBottomCenterOf(pos));
            player.moveTo(Vec3.atBottomCenterOf(pos.south(5)));player.setYRot(180);player.setYHeadRot(180);player.setXRot(0);
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            level.addFreshEntity(player);
            helper.assertTrue(mob.isWatchedBy(player),"Survival observer facing a visible creaking must count as watching");
            mob.tick();helper.assertTrue(mob.isActivated() && mob.isFrozen(),"First gaze must activate and freeze the creaking");
            player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));
            helper.assertTrue(!mob.isWatchedBy(player),"Carved pumpkin must hide the observer's gaze");
            player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);player.setYRot(0);player.setYHeadRot(0);
            mob.tick();helper.assertTrue(mob.canMove(),"Activated creaking must move while observer looks away");
            float health=mob.getHealth();mob.hurt(level.damageSources().playerAttack(player),20);
            helper.assertTrue(mob.isAlive() && mob.getHealth()==health,"Heart-bound creaking must survive player damage");
            int count=resinCount(heart,helper);
            helper.assertTrue(count>=2 && count<=3,"Player hit must produce two or three resin faces");
            mob.hurt(level.damageSources().playerAttack(player),20);
            helper.assertTrue(resinCount(heart,helper)==count,"Repeated hits within five seconds must not duplicate resin");
            level.destroyBlock(heart.getBlockPos(),false);
            helper.assertTrue(mob.isTearingDown(),"Destroying the heart must dismantle its protector");
        } finally {player.discard();if(mob!=null)mob.discard();heart.removeProtector();level.setDayTime(day);}
        helper.succeed();
    }
    private static int resinCount(CreakingHeartBlockEntity heart,GameTestHelper helper) {
        int count=0;
        for(var pos:BlockPos.betweenClosed(heart.getBlockPos().offset(-4,-4,-4),heart.getBlockPos().offset(4,4,4))) {
            var state=helper.getLevel().getBlockState(pos);
            if(state.is(CreakingContent.RESIN_CLUMP.get())) count+=MultifaceBlock.availableFaces(state).size();
        }
        return count;
    }
    @GameTest(template="crop_stand_empty")
    public static void resinRetainsFacesWaterAndCorrectLoot(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(2,3,2));
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos.north(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
        var resin=(ResinClumpBlock)CreakingContent.RESIN_CLUMP.get();
        var state=resin.getStateForPlacement(level.getBlockState(pos),level,pos,Direction.DOWN);level.setBlockAndUpdate(pos,state);
        state=resin.getStateForPlacement(state,level,pos,Direction.NORTH);level.setBlockAndUpdate(pos,state);
        helper.assertTrue(state.getValue(BlockStateProperties.WATERLOGGED),"Resin placed underwater must preserve source water");
        var drops=Block.getDrops(state,level,pos,null,null,ItemStack.EMPTY);
        helper.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum()==2,"Breaking two resin faces must drop exactly two clumps without tools");
        var player=helper.makeMockPlayer();
        state.use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false));
        helper.assertTrue(MultifaceBlock.availableFaces(level.getBlockState(pos)).size()==1,"Hand harvesting must remove one face");
        level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        helper.assertTrue(level.getBlockState(pos).is(Blocks.WATER),"Harvesting the final face must leave its water source");
        var heartState=CreakingContent.HEART.get().defaultBlockState().setValue(CreakingHeartBlock.NATURAL,true);
        var axe=new ItemStack(Items.DIAMOND_AXE);axe.enchant(Enchantments.SILK_TOUCH,1);
        var silk=Block.getDrops(heartState,level,pos,null,null,axe);
        helper.assertTrue(silk.size()==1 && silk.get(0).is(CreakingContent.HEART.get().asItem()) && !silk.get(0).hasTag(),"Silk heart item must not retain natural-only experience state");
        var ordinary=Block.getDrops(heartState,level,pos,null,null,ItemStack.EMPTY);
        helper.assertTrue(ordinary.size()==1 && ordinary.get(0).is(CreakingContent.RESIN_CLUMP.get().asItem()) && ordinary.get(0).getCount()<=3,"Heart without silk touch must drop resin");
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void onlyNaturalPaleTreesHaveHeartDecorator(GameTestHelper helper) {
        var level=helper.getLevel();var registry=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        var natural=(TreeConfiguration)registry.getHolderOrThrow(ForestWorldgen.featureKey("pale_oak")).value().config();
        var grown=(TreeConfiguration)registry.getHolderOrThrow(ForestWorldgen.featureKey("pale_oak_bonemeal")).value().config();
        helper.assertTrue(natural.decorators.stream().anyMatch(d->d instanceof ForestWorldgen.ForestDecorator),"Natural pale tree must include its decorator");
        helper.assertTrue(grown.decorators.isEmpty(),"Player-grown pale trees must never generate hearts");
        var base=helper.absolutePos(new BlockPos(16,3,16));var logs=new HashSet<BlockPos>();for(int y=0;y<7;y++)logs.add(base.above(y));
        boolean generated=false;
        for(int seed=0;seed<200 && !generated;seed++) {
            logs.forEach(p->level.setBlockAndUpdate(p,ForestBlocks.PALE_OAK_LOG.get().defaultBlockState()));
            var context=new TreeDecorator.Context(level,level::setBlockAndUpdate,RandomSource.create(seed*7919L),logs,Set.of(),Set.of());
            for(var decorator:natural.decorators) decorator.place(context);
            for(var pos:logs) if(level.getBlockState(pos).is(CreakingContent.HEART.get())) {
                generated=true;
                helper.assertTrue(level.getBlockState(pos).getValue(CreakingHeartBlock.NATURAL) && CreakingHeartBlock.hasAlignedLogs(level,pos,level.getBlockState(pos)),"Natural heart must be enclosed by aligned pale logs");
            }
        }
        helper.assertTrue(generated,"Natural pale tree decoration must generate a heart across deterministic seeds");
        helper.succeed();
    }
    @GameTest(template="crop_stand_empty")
    public static void coloredEggRecipesAndResinSmithingWork(GameTestHelper helper) {
        var level=helper.getLevel();
        var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player,int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        };
        var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);
        for(var egg:new Item[]{VariantEggs.BLUE_EGG.get(),VariantEggs.BROWN_EGG.get()}) {
            grid.clearContent();
            for(int i=0;i<3;i++) { grid.setItem(i,new ItemStack(Items.MILK_BUCKET));grid.setItem(i+6,new ItemStack(Items.WHEAT)); }
            grid.setItem(3,new ItemStack(Items.SUGAR));grid.setItem(5,new ItemStack(Items.SUGAR));grid.setItem(4,new ItemStack(egg));
            var cake=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);
            helper.assertTrue(cake.isPresent() && cake.get().assemble(grid,level.registryAccess()).is(Items.CAKE),"Both colored eggs must bake vanilla cake");
            grid.clearContent();grid.setItem(0,new ItemStack(Items.PUMPKIN));grid.setItem(1,new ItemStack(Items.SUGAR));grid.setItem(2,new ItemStack(egg));
            var pie=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);
            helper.assertTrue(pie.isPresent() && pie.get().assemble(grid,level.registryAccess()).is(Items.PUMPKIN_PIE),"Both colored eggs must bake vanilla pumpkin pie");
        }
        grid.clearContent();grid.setItem(1,new ItemStack(ForestBlocks.PALE_OAK_LOG.get()));grid.setItem(4,new ItemStack(CreakingContent.RESIN_BLOCK.get()));grid.setItem(7,new ItemStack(ForestBlocks.PALE_OAK_LOG.get()));
        var heart=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);
        helper.assertTrue(heart.isPresent() && heart.get().assemble(grid,level.registryAccess()).is(CreakingContent.HEART.get().asItem()),"Pale logs and resin block must craft a reusable heart");
        var smithing=new SimpleContainer(new ItemStack(Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE),new ItemStack(Items.DIAMOND_CHESTPLATE),new ItemStack(CreakingContent.RESIN_BRICK.get()));
        var trimRecipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING,smithing,level);
        helper.assertTrue(trimRecipe.isPresent(),"Resin bricks must be accepted by vanilla trim smithing");
        var result=trimRecipe.get().assemble(smithing,level.registryAccess());
        var trim=net.minecraft.world.item.armortrim.ArmorTrim.getTrim(level.registryAccess(),result);
        helper.assertTrue(trim.isPresent() && trim.get().material().value().assetName().equals("resin"),"Smithing must write the resin trim material into the armor");
        helper.succeed();
    }
    @GameTest(template="forest_empty")
    public static void existingIllagersGainOneCreakingAvoidanceGoal(GameTestHelper helper) {
        var level=helper.getLevel();
        for(var type:List.of(EntityType.PILLAGER,EntityType.VINDICATOR,EntityType.EVOKER)) {
            var illager=type.create(level);illager.moveTo(Vec3.atCenterOf(helper.absolutePos(new BlockPos(10,3,10))));
            level.addFreshEntity(illager);
            var joined=new net.minecraftforge.event.entity.EntityJoinLevelEvent(illager,level);
            CreakingInteractions.joined(joined);
            long count=illager.goalSelector.getAvailableGoals().stream().filter(goal->goal.getGoal() instanceof CreakingInteractions.FleeCreakingGoal).count();
            helper.assertTrue(count==1,"Each illager must gain exactly one avoidance goal even when re-added");
            illager.discard();
        }
        helper.succeed();
    }

}

package com.y271727uy.farmerstales.gametest;
import com.mojang.authlib.GameProfile;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.fauna.*;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestPlants;
import com.y271727uy.farmerstales.variant.WolfArmorHolder;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("farmerstales") @PrefixGameTestTemplate(false)
public final class FaunaGameTests {
    @GameTest(template="forest_empty") public static void potsAcceptAndReturnPlantsAndEyeblossomsChangeWithDaylight(GameTestHelper h){
        var level=h.getLevel();var player=h.makeMockPlayer();int offset=0;long time=level.getDayTime();
        try{for(var entry:FaunaContent.POTS.entrySet()){
            var pos=h.absolutePos(new BlockPos(4+offset*5,2,4));offset++;level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,Blocks.FLOWER_POT.defaultBlockState());
            var plant=ForestBlocks.ALL.get(entry.getKey()).get();player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(plant));
            level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            h.assertTrue(level.getBlockState(pos).is(entry.getValue().get()),"Vanilla flower pot must accept the new plant");
            if(entry.getKey().contains("eyeblossom")){
                boolean open=entry.getKey().startsWith("open");level.setDayTime(open?6000:18000);level.updateSkyBrightness();
                entry.getValue().get().randomTick(level.getBlockState(pos),level,pos,level.random);
                h.assertTrue(level.getBlockState(pos).is(FaunaContent.POTS.get(open?"closed_eyeblossom":"open_eyeblossom").get()),"Potted eyeblossom must change with daylight");
            }
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            level.getBlockState(pos).use(level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            h.assertTrue(level.getBlockState(pos).is(Blocks.FLOWER_POT)&&!player.getMainHandItem().isEmpty(),"Empty hand must retrieve the plant and leave the pot");
        }}finally{level.setDayTime(time);level.updateSkyBrightness();}h.succeed();
    }
    @GameTest(template="forest_empty") public static void eyeblossomsScheduleAChangeWaveAndPoisonOnlyOnOpenContact(GameTestHelper h){
        var level=h.getLevel();long time=level.getDayTime();var pos=h.absolutePos(new BlockPos(10,2,10));var closed=(ForestPlants.Eyeblossom)ForestBlocks.CLOSED_EYEBLOSSOM.get();var open=ForestBlocks.OPEN_EYEBLOSSOM.get();
        try{
            for(int x=0;x<5;x++){level.setBlockAndUpdate(pos.east(x).below(),Blocks.GRASS_BLOCK.defaultBlockState());level.setBlockAndUpdate(pos.east(x),closed.defaultBlockState());}
            level.setDayTime(18000);level.updateSkyBrightness();closed.randomTick(closed.defaultBlockState(),level,pos,level.random);
            h.assertTrue(level.getBlockState(pos).is(open),"Root flower must open at night");
            h.assertTrue(level.getBlockTicks().hasScheduledTick(pos.east(3),closed)&&!level.getBlockTicks().hasScheduledTick(pos.east(4),closed),"Opening wave must schedule matching flowers within three blocks");
            closed.tick(closed.defaultBlockState(),level,pos.east(),level.random);h.assertTrue(level.getBlockState(pos.east()).is(open),"Scheduled wave tick must open its flower");
            var bee=EntityType.BEE.create(level);open.entityInside(open.defaultBlockState(),level,pos,bee);
            h.assertTrue(bee.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Open eyeblossom contact must poison a bee");bee.removeAllEffects();
            closed.entityInside(closed.defaultBlockState(),level,pos,bee);h.assertTrue(!bee.hasEffect(net.minecraft.world.effect.MobEffects.POISON),"Closed eyeblossom must not poison a bee");
            h.assertTrue(!bee.isFood(new ItemStack(closed))&&bee.isFood(new ItemStack(open)),"Bees must be attracted only to the open flower");bee.discard();
        }finally{level.setDayTime(time);level.updateSkyBrightness();}h.succeed();
    }
    @GameTest(template="forest_empty") public static void armadillosBrushShedAndRetainTheirTimers(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,3,10));var animal=FaunaContent.ARMADILLO.get().create(level);animal.moveTo(Vec3.atCenterOf(pos));
        // Remove previous-run item entities: failed tests can leave them in the reusable world.
        level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)).forEach(Entity::discard);
        var player=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"armadillo-brush"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BRUSH));
        animal.mobInteract(player,InteractionHand.MAIN_HAND);
        h.assertTrue(player.getMainHandItem().getDamageValue()==16,"Brushing must use sixteen brush durability");
        var data=animal.saveWithoutId(new CompoundTag());data.putInt("ScuteTime",0);animal.readAdditionalSaveData(data);animal.tick();
        var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(FaunaContent.SCUTE.get())).mapToInt(e->e.getItem().getCount()).sum()==2,"Brushing and a completed shedding timer must each yield one scute: drops="+drops.stream().map(e->e.getItem().toString()).toList()+", time="+animal.saveWithoutId(new CompoundTag()).getInt("ScuteTime")+", alive="+animal.isAlive()+", baby="+animal.isBaby()+", loot="+level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT));
        var saved=animal.saveWithoutId(new CompoundTag());h.assertTrue(saved.getInt("ScuteTime")>=6000&&saved.getInt("ScuteTime")<12000,"Shedding timer must reset to five to ten minutes");
        animal.setAge(-24000);int durability=player.getMainHandItem().getDamageValue();animal.mobInteract(player,InteractionHand.MAIN_HAND);h.assertTrue(player.getMainHandItem().getDamageValue()==durability,"Baby armadillos must not yield brushed scutes");
        animal.discard();player.discard();h.succeed();
    }
    @GameTest(template="forest_empty") public static void armadillosRollForThreatsReduceDamageAndCanBreed(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(10,3,10));var animal=FaunaContent.ARMADILLO.get().create(level);animal.moveTo(Vec3.atCenterOf(pos));
        var zombie=EntityType.ZOMBIE.create(level);zombie.moveTo(Vec3.atCenterOf(pos.east(3)));
        h.assertTrue(animal.scaredBy(zombie),"Nearby undead must scare armadillos");zombie.discard();animal.roll();
        float health=animal.getHealth();animal.hurt(level.damageSources().generic(),5);h.assertTrue(animal.getHealth()==health-2,"Rolled armadillo must reduce damage by one then halve it");
        var saved=animal.saveWithoutId(new CompoundTag());var copy=FaunaContent.ARMADILLO.get().create(level);copy.readAdditionalSaveData(saved);h.assertTrue(copy.rolled(),"Rolled state must survive save/load");
        h.assertTrue(animal.isFood(new ItemStack(Items.SPIDER_EYE))&&!animal.canFallInLove(),"Spider eyes are food, but rolled armadillos cannot breed");animal.changeState(0);
        h.assertTrue(animal.getBreedOffspring(level,copy).getType()==FaunaContent.ARMADILLO.get(),"Breeding must create the registered armadillo type");animal.discard();copy.discard();h.succeed();
    }
    private static void interact(net.minecraft.world.entity.player.Player player,Wolf wolf,ItemStack stack){player.setItemInHand(InteractionHand.MAIN_HAND,stack);MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteract(player,InteractionHand.MAIN_HAND,wolf));}
    @GameTest(template="forest_empty") public static void wolfArmorAbsorbsRepairsDyesSavesAndCanBeRemoved(GameTestHelper h){
        var level=h.getLevel();var wolf=EntityType.WOLF.create(level);wolf.tame(h.makeMockPlayer());
        var player=FakePlayerFactory.get(level,new GameProfile(wolf.getOwnerUUID(),"armor-owner"));player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var holder=(WolfArmorHolder)wolf;var armor=new ItemStack(FaunaContent.WOLF_ARMOR.get());((DyeableLeatherItem)armor.getItem()).setColor(armor,0xAA2233);
        interact(player,wolf,armor);h.assertTrue(holder.farmerstales$getWolfArmor().is(FaunaContent.WOLF_ARMOR.get())&&player.getMainHandItem().isEmpty(),"Owner must equip and consume one armor");
        float health=wolf.getHealth();wolf.hurt(level.damageSources().generic(),12);h.assertTrue(wolf.getHealth()==health&&holder.farmerstales$getWolfArmor().getDamageValue()==12,"Armor must absorb physical damage into durability");
        interact(player,wolf,new ItemStack(FaunaContent.SCUTE.get(),2));h.assertTrue(holder.farmerstales$getWolfArmor().getDamageValue()==4&&player.getMainHandItem().getCount()==1,"One scute must repair eight durability");
        var copy=EntityType.WOLF.create(level);copy.readAdditionalSaveData(wolf.saveWithoutId(new CompoundTag()));var persisted=((WolfArmorHolder)copy).farmerstales$getWolfArmor();
        h.assertTrue(persisted.getDamageValue()==4&&((DyeableLeatherItem)persisted.getItem()).getColor(persisted)==0xAA2233,"Armor durability and dye must persist with the wolf");
        interact(player,wolf,new ItemStack(Items.SHEARS));h.assertTrue(holder.farmerstales$getWolfArmor().isEmpty(),"Owner shears must remove armor");wolf.discard();copy.discard();player.discard();h.succeed();
    }
    @GameTest(template="forest_empty") public static void armorBypassAndBreakAndOwnershipFollowRules(GameTestHelper h){
        var level=h.getLevel();var owner=h.makeMockPlayer();var wolf=EntityType.WOLF.create(level);wolf.tame(owner);var holder=(WolfArmorHolder)wolf;holder.farmerstales$setWolfArmor(new ItemStack(FaunaContent.WOLF_ARMOR.get()));
        float health=wolf.getHealth();wolf.hurt(level.damageSources().magic(),2);h.assertTrue(wolf.getHealth()<health&&holder.farmerstales$getWolfArmor().getDamageValue()==0,"Magic bypass must damage wolf without consuming armor");
        var intruder=h.makeMockPlayer();interact(intruder,wolf,new ItemStack(Items.SHEARS));h.assertTrue(!holder.farmerstales$getWolfArmor().isEmpty(),"Other players cannot shear owned armor");
        wolf.invulnerableTime=0;wolf.hurt(level.damageSources().generic(),100);h.assertTrue(wolf.isAlive()&&holder.farmerstales$getWolfArmor().isEmpty(),"A breaking armor must absorb the complete hit and then disappear");wolf.discard();h.succeed();
    }
    @GameTest(template="forest_empty") public static void armadilloSpawnsAreRegisteredInSixOriginalBiomes(GameTestHelper h){
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for(String biome:new String[]{"savanna","savanna_plateau","windswept_savanna","badlands","wooded_badlands","eroded_badlands"}){
            var data=registry.get(new ResourceLocation("minecraft",biome)).getMobSettings().getMobs(MobCategory.CREATURE).unwrap();
            h.assertTrue(data.stream().anyMatch(s->s.type==FaunaContent.ARMADILLO.get()&&s.minCount>=1&&s.maxCount>=s.minCount),"Biome must have real armadillo spawn registration: "+biome);
        }
        h.succeed();
    }
}

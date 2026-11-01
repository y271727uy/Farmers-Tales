package com.y271727uy.farmerstales.gameplay.fauna;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import java.util.*;
public final class FaunaContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final Map<String,RegistryObject<Block>> POTS=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<SoundEvent>> SOUNDS_BY_NAME=new LinkedHashMap<>();
    public static final java.util.function.Supplier<Item> SCUTE=com.y271727uy.farmerstales.all.ModItems.ARMADILLO_SCUTE;
    public static final RegistryObject<Item> WOLF_ARMOR=ITEMS.register("wolf_armor",()->new WolfArmorItem(new Item.Properties().durability(64)));
    public static final java.util.function.Supplier<EntityType<com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity>> ARMADILLO=()->com.y271727uy.farmerstales.entity.ModEntities.ARMADILLO.getType().get();
    public static final java.util.function.Supplier<Item> SPAWN_EGG=()->com.y271727uy.farmerstales.entity.ModEntities.ARMADILLO.getSpawnEgg().get();
    static {
        for(String name:new String[]{"pale_oak_sapling","poplar_sapling","open_eyeblossom","closed_eyeblossom"})
            POTS.put(name,BLOCKS.register("potted_"+name,()->new FlowerPotBlock(()->(FlowerPotBlock)Blocks.FLOWER_POT,()->ForestBlocks.ALL.get(name).get(),BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).randomTicks()){
                @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
                    if(name.contains("eyeblossom")&&level.dimensionType().natural()&&level.isNight()!=name.startsWith("open"))level.setBlockAndUpdate(pos,POTS.get(level.isNight()?"open_eyeblossom":"closed_eyeblossom").get().defaultBlockState());
                }
            }));
        for(String name:new String[]{"ambient","brush","death","eat","hurt","hurt_reduced","land","peek","roll","scute_drop","step","unroll_finish","unroll_start"})sound("entity.armadillo."+name);
        for(String name:new String[]{"repair","break","damage","crack"})sound("item.wolf_armor."+name);
    }
    static { sound("item.armor.equip_wolf"); sound("item.armor.unequip_wolf"); }
    private static void sound(String name){SOUNDS_BY_NAME.put(name,SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name))));}
    public static SoundEvent soundEvent(String name){return SOUNDS_BY_NAME.get(name.replace("item.wolf_armor.equip","item.armor.equip_wolf").replace("item.wolf_armor.unequip","item.armor.unequip_wolf")).get();}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);SOUNDS.register(bus);
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->{
            POTS.forEach((name,pot)->((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name),pot));
        }));
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,p->{p.add("item.farmerstales.wolf_armor","Wolf Armor");});
    }
}

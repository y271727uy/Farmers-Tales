package com.y271727uy.farmerstales.gameplay.building;

import com.y271727uy.farmerstales.FTMod;
import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import java.util.*;
import java.util.function.Supplier;

public final class BuildingContent {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,FTMod.MODID);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> PAUSE=PARTICLES.register("pause_mob_growth",()->new net.minecraft.core.particles.SimpleParticleType(false)),RESET=PARTICLES.register("reset_mob_growth",()->new net.minecraft.core.particles.SimpleParticleType(false));
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final Map<String,RegistryObject<Block>> TUFF=new LinkedHashMap<>();
    public static final RegistryObject<Block> GOLDEN_DANDELION=block("golden_dandelion",()->new FlowerBlock(()->MobEffects.SATURATION,7,BlockBehaviour.Properties.copy(Blocks.DANDELION)));
    public static final RegistryObject<Block> POTTED_GOLDEN_DANDELION=BLOCKS.register("potted_golden_dandelion",()->new FlowerPotBlock(()->(FlowerPotBlock)Blocks.FLOWER_POT,GOLDEN_DANDELION,BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION)));
    public static final RegistryObject<SoundEvent> USE=sound("item.golden_dandelion.use"),UNUSE=sound("item.golden_dandelion.unuse");
    private static final Map<String,RegistryObject<SoundEvent>> TUFF_SOUNDS=new LinkedHashMap<>();
    static {
        for(String prefix:new String[]{"block.polished_tuff.","block.tuff_bricks."})for(String action:new String[]{"break","step","place","hit","fall"})TUFF_SOUNDS.put(prefix+action,sound(prefix+action));
        family("tuff",()->Blocks.TUFF,false);
        family("polished_tuff",()->Blocks.TUFF,true);
        family("tuff_brick",()->TUFF.get("tuff_bricks").get(),false);
    }
    private static BlockBehaviour.Properties properties(String name){
        String prefix=name.startsWith("polished_")?"block.polished_tuff.":name.contains("brick")?"block.tuff_bricks.":null;
        SoundType sound=prefix==null?SoundType.TUFF:new net.minecraftforge.common.util.ForgeSoundType(1,1,()->TUFF_SOUNDS.get(prefix+"break").get(),()->TUFF_SOUNDS.get(prefix+"step").get(),()->TUFF_SOUNDS.get(prefix+"place").get(),()->TUFF_SOUNDS.get(prefix+"hit").get(),()->TUFF_SOUNDS.get(prefix+"fall").get());
        return BlockBehaviour.Properties.copy(Blocks.TUFF).sound(sound);
    }
    private static RegistryObject<Block> block(String name,Supplier<Block> supplier){
        var block=BLOCKS.register(name,supplier);ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    private static void family(String name,Supplier<Block> base,boolean polished){
        if(polished)TUFF.put(name,block(name,()->new Block(properties(name))));
        if(name.equals("tuff"))TUFF.put("tuff_bricks",block("tuff_bricks",()->new Block(properties("tuff_bricks"))));
        Supplier<Block> stairBase=polished?()->TUFF.get(name).get():base;
        TUFF.put(name+"_stairs",block(name+"_stairs",()->new StairBlock(()->stairBase.get().defaultBlockState(),properties(name))));
        TUFF.put(name+"_slab",block(name+"_slab",()->new SlabBlock(properties(name))));
        TUFF.put(name+"_wall",block(name+"_wall",()->new WallBlock(properties(name))));
        if(!polished){String chiseled=name.equals("tuff")?"chiseled_tuff":"chiseled_tuff_bricks";TUFF.put(chiseled,block(chiseled,()->new Block(properties(name))));}
    }
    private static RegistryObject<SoundEvent> sound(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name)));}
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);SOUNDS.register(bus);PARTICLES.register(bus);
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,"golden_dandelion"),POTTED_GOLDEN_DANDELION)));
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,p->{
            TUFF.keySet().forEach(name->p.add("block.farmerstales."+name,Arrays.stream(name.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "))));
            p.add("block.farmerstales.golden_dandelion","Golden Dandelion");p.add("block.farmerstales.potted_golden_dandelion","Potted Golden Dandelion");
        });
    }
}

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

/** Golden dandelion (26.1): pauses or resumes the growth of baby animals. */
@com.y271727uy.farmerstales.backport.Backport(com.y271727uy.farmerstales.backport.BackportFeature.GOLDEN_DANDELION)
public final class BuildingContent {
    private static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,FTMod.MODID);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> PAUSE=PARTICLES.register("pause_mob_growth",()->new net.minecraft.core.particles.SimpleParticleType(false)),RESET=PARTICLES.register("reset_mob_growth",()->new net.minecraft.core.particles.SimpleParticleType(false));
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final RegistryObject<Block> GOLDEN_DANDELION=BLOCKS.register("golden_dandelion",()->new FlowerBlock(()->MobEffects.SATURATION,7,BlockBehaviour.Properties.copy(Blocks.DANDELION)));
    static { ITEMS.register("golden_dandelion",()->new BlockItem(GOLDEN_DANDELION.get(),new Item.Properties())); }
    public static final RegistryObject<Block> POTTED_GOLDEN_DANDELION=BLOCKS.register("potted_golden_dandelion",()->new FlowerPotBlock(()->(FlowerPotBlock)Blocks.FLOWER_POT,GOLDEN_DANDELION,BlockBehaviour.Properties.copy(Blocks.POTTED_DANDELION)));
    public static final RegistryObject<SoundEvent> USE=sound("item.golden_dandelion.use"),UNUSE=sound("item.golden_dandelion.unuse");
    private BuildingContent(){}
    private static RegistryObject<SoundEvent> sound(String name){return SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name)));}
    /** Called by BackportLoader when GOLDEN_DANDELION is enabled. */
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);SOUNDS.register(bus);PARTICLES.register(bus);
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,"golden_dandelion"),POTTED_GOLDEN_DANDELION)));
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,p->{
            p.add("block.farmerstales.golden_dandelion","Golden Dandelion");p.add("block.farmerstales.potted_golden_dandelion","Potted Golden Dandelion");
        });
    }
}

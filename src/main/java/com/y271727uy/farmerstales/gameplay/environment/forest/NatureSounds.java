package com.y271727uy.farmerstales.gameplay.environment.forest;
import com.y271727uy.farmerstales.FTMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;
public final class NatureSounds {
    private static final DeferredRegister<SoundEvent> REGISTER=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final Map<String,RegistryObject<SoundEvent>> ALL=new LinkedHashMap<>();
    static {
        for(String name:new String[]{"block.leaf_litter.break","block.leaf_litter.step","block.leaf_litter.place","block.leaf_litter.hit","block.leaf_litter.fall","block.dry_grass.ambient","block.sand.idle","block.pale_hanging_moss.idle","block.eyeblossom.idle","block.eyeblossom.open","block.eyeblossom.close","block.eyeblossom.open_long","block.eyeblossom.close_long"}) add(name);
        for(String variant:new String[]{"angry","big","cute","grumpy","puglin","sad"})for(String sound:new String[]{"ambient","death","growl","hurt","pant","whine"})add("entity.wolf_"+variant+"."+sound);
    }
    private static void add(String name){ALL.put(name,REGISTER.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name))));}
    public static SoundEvent get(String name){return ALL.get(name).get();}
    public static SoundType litter(){return new net.minecraftforge.common.util.ForgeSoundType(1,1,()->get("block.leaf_litter.break"),()->get("block.leaf_litter.step"),()->get("block.leaf_litter.place"),()->get("block.leaf_litter.hit"),()->get("block.leaf_litter.fall"));}
    public static void register(IEventBus bus){
        REGISTER.register(bus);
        com.y271727uy.farmerstales.registrate.ModRegistrate.REGISTRATE.addDataGenerator(com.tterrag.registrate.providers.ProviderType.LANG,provider->{
            provider.add("subtitles.farmerstales.block.dry_grass.ambient","Windy sounds");
            provider.add("subtitles.farmerstales.block.sand.idle","Sandy sounds");
            provider.add("subtitles.farmerstales.block.eyeblossom.open","Eyeblossom opens");
            provider.add("subtitles.farmerstales.block.eyeblossom.close","Eyeblossom closes");
            provider.add("subtitles.farmerstales.entity.wolf.bark","Wolf barks");
            provider.add("subtitles.farmerstales.entity.wolf.death","Wolf dies");
            provider.add("subtitles.farmerstales.entity.wolf.growl","Wolf growls");
            provider.add("subtitles.farmerstales.entity.wolf.hurt","Wolf hurts");
            provider.add("subtitles.farmerstales.entity.wolf.pant","Wolf pants");
            provider.add("subtitles.farmerstales.entity.wolf.whine","Wolf whines");
        });
    }
}

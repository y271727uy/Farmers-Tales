package com.y271727uy.farmerstales.gameplay.fauna;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.backport.Backport;
import com.y271727uy.farmerstales.backport.BackportFeature;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;
/** Armored Paws (1.20.5): armadillo, armadillo scute and wolf armor. */
@Backport(BackportFeature.ARMORED_PAWS)
public final class FaunaContent {
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final Map<String,RegistryObject<SoundEvent>> SOUNDS_BY_NAME=new LinkedHashMap<>();
    /** Kept separate from the turtle scute so recipes and loot can target armadillos precisely. */
    public static final ItemEntry<Item> SCUTE=ModRegistrate.REGISTRATE.item("armadillo_scute",Item::new)
            .model((context,provider)->provider.generated(context,provider.modLoc("item/armadillo_scute"))).register();
    public static final RegistryObject<Item> WOLF_ARMOR=ITEMS.register("wolf_armor",()->new WolfArmorItem(new Item.Properties().durability(64)));
    public static final java.util.function.Supplier<EntityType<com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity>> ARMADILLO=()->com.y271727uy.farmerstales.entity.ModEntities.ARMADILLO.getType().get();
    public static final java.util.function.Supplier<Item> SPAWN_EGG=()->com.y271727uy.farmerstales.entity.ModEntities.ARMADILLO.getSpawnEgg().get();
    static {
        for(String name:new String[]{"ambient","brush","death","eat","hurt","hurt_reduced","land","peek","roll","scute_drop","step","unroll_finish","unroll_start"})sound("entity.armadillo."+name);
        for(String name:new String[]{"repair","break","damage","crack"})sound("item.wolf_armor."+name);
        sound("item.armor.equip_wolf"); sound("item.armor.unequip_wolf");
    }
    private FaunaContent(){}
    private static void sound(String name){SOUNDS_BY_NAME.put(name,SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name))));}
    public static SoundEvent soundEvent(String name){return SOUNDS_BY_NAME.get(name.replace("item.wolf_armor.equip","item.armor.equip_wolf").replace("item.wolf_armor.unequip","item.armor.unequip_wolf")).get();}
    /** Called by BackportLoader when {@link BackportFeature#ARMORED_PAWS} is enabled. */
    public static void register(IEventBus bus){
        ITEMS.register(bus);SOUNDS.register(bus);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,p->{p.add("item.farmerstales.wolf_armor","Wolf Armor");});
    }
}

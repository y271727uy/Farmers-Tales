package com.y271727uy.farmerstales.gameplay.shelf;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

public final class ShelfContent {
    private static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,FTMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,FTMod.MODID);
    public static final Map<String,RegistryObject<ShelfBlock>> SHELVES=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<SoundEvent>> SOUNDS_BY_NAME=new LinkedHashMap<>();
    public static final RegistryObject<BlockEntityType<ShelfBlockEntity>> SHELF=BLOCK_ENTITIES.register("shelf",()->BlockEntityType.Builder.of(ShelfBlockEntity::new,SHELVES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    static {
        for(String wood:new String[]{"oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped","pale_oak","poplar"}){
            var shelf=BLOCKS.register(wood+"_shelf",()->new ShelfBlock(BlockBehaviour.Properties.copy(planks(wood)).strength(2,3).sound(shelfSound()).noOcclusion(),!wood.equals("crimson")&&!wood.equals("warped")));
            SHELVES.put(wood,shelf);ITEMS.register(wood+"_shelf",()->new BlockItem(shelf.get(),new Item.Properties()){
                @Override public int getBurnTime(ItemStack stack,net.minecraft.world.item.crafting.RecipeType<?> type){return wood.equals("crimson")||wood.equals("warped")?0:300;}
            });
        }
        for(String action:new String[]{"activate","deactivate","place_item","take_item","single_swap","multi_swap","break","step","place","hit","fall"}){
            String name="block.shelf."+action;SOUNDS_BY_NAME.put(action,SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,name))));
        }
    }
    private static SoundType shelfSound(){return new net.minecraftforge.common.util.ForgeSoundType(1,1,()->SOUNDS_BY_NAME.get("break").get(),()->SOUNDS_BY_NAME.get("step").get(),()->SOUNDS_BY_NAME.get("place").get(),()->SOUNDS_BY_NAME.get("hit").get(),()->SOUNDS_BY_NAME.get("fall").get());}
    private static Block planks(String wood){
        if(wood.equals("pale_oak")||wood.equals("poplar"))return ForestBlocks.ALL.get(wood+"_planks").get();
        return ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("minecraft",wood+"_planks"));
    }
    public static void register(IEventBus bus){
        BLOCKS.register(bus);ITEMS.register(bus);BLOCK_ENTITIES.register(bus);SOUNDS.register(bus);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG,p->SHELVES.keySet().forEach(wood->p.add("block.farmerstales."+wood+"_shelf",Arrays.stream(wood.split("_")).map(s->Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "))+" Shelf")));
    }
}

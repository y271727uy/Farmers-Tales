package com.y271727uy.farmerstales.gameplay.creaking;

import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class CreakingContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FTMod.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FTMod.MODID);
    public static final Map<String, RegistryObject<Block>> ALL_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, RegistryObject<SoundEvent>> ALL_SOUNDS = new LinkedHashMap<>();

    public static final RegistryObject<Block> HEART = block("creaking_heart", CreakingHeartBlock::new);
    public static final RegistryObject<Block> RESIN_CLUMP = block("resin_clump", ResinClumpBlock::new);
    public static final RegistryObject<Block> RESIN_BLOCK = block("resin_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.HONEYCOMB_BLOCK).instabreak().sound(blockSounds("block.resin"))));
    public static final RegistryObject<Block> RESIN_BRICKS = block("resin_bricks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS).sound(blockSounds("block.resin_bricks"))));
    public static final RegistryObject<Block> RESIN_BRICK_STAIRS = block("resin_brick_stairs", () -> new StairBlock(() -> RESIN_BRICKS.get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.BRICK_STAIRS).sound(blockSounds("block.resin_bricks"))));
    public static final RegistryObject<Block> RESIN_BRICK_SLAB = block("resin_brick_slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.BRICK_SLAB).sound(blockSounds("block.resin_bricks"))));
    public static final RegistryObject<Block> RESIN_BRICK_WALL = block("resin_brick_wall", () -> new WallBlock(BlockBehaviour.Properties.copy(Blocks.BRICK_WALL).sound(blockSounds("block.resin_bricks"))));
    public static final RegistryObject<Block> CHISELED_RESIN_BRICKS = block("chiseled_resin_bricks", () -> new Block(BlockBehaviour.Properties.copy(Blocks.BRICKS).sound(blockSounds("block.resin_bricks"))));
    public static final RegistryObject<Item> RESIN_BRICK = ITEMS.register("resin_brick", () -> new Item(new Item.Properties()));
    public static final RegistryObject<EntityType<CreakingEntity>> CREAKING = ENTITIES.register("creaking", () -> EntityType.Builder
            .of(CreakingEntity::new, MobCategory.MONSTER).sized(0.9F, 2.7F).fireImmune().clientTrackingRange(10).build("farmerstales:creaking"));
    public static final RegistryObject<Item> SPAWN_EGG = ITEMS.register("creaking_spawn_egg", () -> new ForgeSpawnEggItem(CREAKING, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<BlockEntityType<CreakingHeartBlockEntity>> HEART_ENTITY = BLOCK_ENTITIES.register("creaking_heart", () ->
            BlockEntityType.Builder.of(CreakingHeartBlockEntity::new, HEART.get()).build(null));

    static {
        for (String name : new String[]{"entity.creaking.ambient", "entity.creaking.activate", "entity.creaking.deactivate", "entity.creaking.attack",
                "entity.creaking.step", "entity.creaking.sway", "entity.creaking.death", "block.creaking_heart.idle", "block.creaking_heart.hurt", "block.creaking_heart.spawn", "entity.creaking.spawn",
                "entity.creaking.freeze", "entity.creaking.unfreeze", "entity.creaking.twitch", "block.creaking_heart.break", "block.creaking_heart.step",
                "block.creaking_heart.place", "block.creaking_heart.hit", "block.creaking_heart.fall",
                "block.resin.break", "block.resin.fall", "block.resin.place", "block.resin.step",
                "block.resin_bricks.break", "block.resin_bricks.fall", "block.resin_bricks.hit", "block.resin_bricks.place", "block.resin_bricks.step"}) {
            ALL_SOUNDS.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, name))));
        }
    }

    public static net.minecraft.world.level.block.SoundType blockSounds(String prefix) {
        return new net.minecraftforge.common.util.ForgeSoundType(1, 1,
                () -> sound(prefix + ".break"), () -> sound(prefix + ".step"), () -> sound(prefix + ".place"),
                () -> sound(prefix.equals("block.resin") ? prefix + ".place" : prefix + ".hit"), () -> sound(prefix + ".fall"));
    }
    private CreakingContent() {}
    private static RegistryObject<Block> block(String name, Supplier<Block> factory) {
        var block = BLOCKS.register(name, factory);
        ALL_BLOCKS.put(name, block);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
    public static SoundEvent sound(String name) { return ALL_SOUNDS.get(name).get(); }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus); BLOCK_ENTITIES.register(bus); SOUNDS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) -> event.put(CREAKING.get(), CreakingEntity.attributes().build()));
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            ALL_BLOCKS.keySet().forEach(name -> provider.add("block.farmerstales." + name, title(name)));
            provider.add("item.farmerstales.resin_brick", "Resin Brick");
            provider.add("item.farmerstales.creaking_spawn_egg", "Creaking Spawn Egg");
            provider.add("entity.farmerstales.creaking", "Creaking");
            provider.add("trim_material.farmerstales.resin", "Resin Material");
            provider.add("subtitles.farmerstales.block.creaking_heart.hurt", "Creaking Heart grumbles");
            provider.add("subtitles.farmerstales.block.creaking_heart.spawn", "Creaking Heart awakens");
            provider.add("subtitles.farmerstales.entity.creaking.activate", "Creaking watches");
            provider.add("subtitles.farmerstales.entity.creaking.ambient", "Creaking creaks");
            provider.add("subtitles.farmerstales.entity.creaking.attack", "Creaking attacks");
            provider.add("subtitles.farmerstales.entity.creaking.deactivate", "Creaking calms");
            provider.add("subtitles.farmerstales.entity.creaking.death", "Creaking crumbles");
            provider.add("subtitles.farmerstales.entity.creaking.freeze", "Creaking stops");
            provider.add("subtitles.farmerstales.entity.creaking.spawn", "Creaking manifests");
            provider.add("subtitles.farmerstales.entity.creaking.sway", "Creaking is hit");
            provider.add("subtitles.farmerstales.entity.creaking.twitch", "Creaking twitches");
            provider.add("subtitles.farmerstales.entity.creaking.unfreeze", "Creaking moves");
        });
    }

    private static String title(String name) {
        if (name.equals("resin_block")) return "Block of Resin";
        var result = new StringBuilder();
        for (String word : name.split("_")) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}

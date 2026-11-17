package com.y271727uy.farmerstales.all;

import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/** Shared block/item factories for the forest tree families (pale oak, poplar). */
public final class WoodBlockRegistrar {
    private final DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, FTMod.MODID);
    private final DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID);
    /** Blocks that have an item, keyed by registry path. */
    public final Map<String, RegistryObject<Block>> all = new LinkedHashMap<>();
    public final Map<String, RegistryObject<Block>> building = new LinkedHashMap<>();
    public final Map<String, RegistryObject<Block>> pots = new LinkedHashMap<>();

    public RegistryObject<Block> add(String name, Supplier<Block> factory) {
        var block = blocks.register(name, factory);
        all.put(name, block);
        items.register(name, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) { return burnTime(name); }
        });
        return block;
    }

    private int burnTime(String name) {
        if (name.equals("leaf_litter")) return 100;
        if (building.containsKey(name)) return name.endsWith("slab") ? 150 : name.endsWith("button") ? 100 : name.endsWith("door") && !name.endsWith("trapdoor") ? 200 : 300;
        if (name.contains("sapling")) return 100;
        if (name.contains("log") || name.contains("wood") || name.endsWith("planks")) return 300;
        return -1;
    }

    public RegistryObject<Block> wood(String name, String stripped) {
        return add(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)) {
            @Override public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action, boolean simulate) {
                if (stripped != null && action == ToolActions.AXE_STRIP && context.getItemInHand().canPerformAction(action)) {
                    return all.get(stripped).get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
                }
                return super.getToolModifiedState(state, context, action, simulate);
            }
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
        });
    }

    public RegistryObject<Block> leaves(String name) {
        return add(name, () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.getValue(WATERLOGGED) ? 0 : 60; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.getValue(WATERLOGGED) ? 0 : 30; }
        });
    }

    public RegistryObject<Block> planks(String name) {
        return add(name, () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 20; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
        });
    }

    /** Stairs, slab, fence, fence gate, door, trapdoor, button and pressure plate for {@code tree}. */
    public void buildingSet(String tree) {
        building(tree, "stairs", () -> new StairBlock(() -> all.get(tree + "_planks").get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state); }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state) == 0 ? 0 : 5; }
        });
        building(tree, "slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state); }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state) == 0 ? 0 : 5; }
        });
        building(tree, "fence", () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state); }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state) == 0 ? 0 : 5; }
        });
        building(tree, "fence_gate", () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE), WoodType.OAK) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state); }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return waterloggedFlammability(state) == 0 ? 0 : 5; }
        });
        building(tree, "door", () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR), BlockSetType.OAK));
        building(tree, "trapdoor", () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR), BlockSetType.OAK));
        building(tree, "button", () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON), BlockSetType.OAK, 30, true));
        building(tree, "pressure_plate", () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE), BlockSetType.OAK));
    }

    private static int waterloggedFlammability(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED) ? 0 : 20;
    }

    private void building(String tree, String part, Supplier<Block> factory) {
        String name = tree + "_" + part;
        building.put(name, add(name, factory));
    }

    /** A flower pot for {@code plant}; {@code factory} builds the pot block from the plant supplier. */
    public RegistryObject<Block> pot(String plant, Supplier<Block> factory) {
        var pot = blocks.register("potted_" + plant, factory);
        pots.put(plant, pot);
        return pot;
    }

    public void register(IEventBus bus, Map<String, String> extraLang) {
        blocks.register(bus);
        items.register(bus);
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            pots.forEach((plant, pot) -> ((FlowerPotBlock) Blocks.FLOWER_POT)
                    .addPlant(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, plant), pot));
            all.forEach((name, block) -> {
                if (name.contains("leaves") || name.contains("sapling") || name.contains("moss")
                        || name.contains("eyeblossom") || name.equals("red_shrub") || name.equals("shelf_mushroom")) {
                    ComposterBlock.COMPOSTABLES.put(block.get().asItem(), name.contains("eyeblossom")
                            || name.equals("pale_moss_block") || name.equals("shelf_mushroom") ? 0.65F : 0.3F);
                }
            });
        }));
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            all.keySet().forEach(name -> provider.add("block." + FTMod.MODID + "." + name, title(name)));
            extraLang.forEach(provider::add);
        });
    }

    public static String title(String name) {
        var title = new StringBuilder();
        for (String word : name.split("_")) {
            if (!title.isEmpty()) title.append(' ');
            title.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
        }
        return title.toString();
    }
}

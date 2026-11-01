package com.y271727uy.farmerstales.all;

import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestPlants;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestLeafParticles;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
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

/** Natural blocks and renewable tree materials belonging to the two forest biomes. */
public final class ForestBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID);
    public static final Map<String, RegistryObject<Block>> ALL = new LinkedHashMap<>();

    public static final RegistryObject<Block> PALE_OAK_LOG = wood("pale_oak_log", "stripped_pale_oak_log");
    public static final RegistryObject<Block> STRIPPED_PALE_OAK_LOG = wood("stripped_pale_oak_log", null);
    public static final RegistryObject<Block> PALE_OAK_WOOD = wood("pale_oak_wood", "stripped_pale_oak_wood");
    public static final RegistryObject<Block> STRIPPED_PALE_OAK_WOOD = wood("stripped_pale_oak_wood", null);
    public static final RegistryObject<Block> PALE_OAK_PLANKS = planks("pale_oak_planks");
    public static final RegistryObject<Block> PALE_OAK_LEAVES = leaves("pale_oak_leaves");
    public static final RegistryObject<Block> PALE_OAK_SAPLING = add("pale_oak_sapling", () -> new SaplingBlock(new AbstractMegaTreeGrower() {
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) { return null; }
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random) { return ForestWorldgen.featureKey("pale_oak_bonemeal"); }
    }, BlockBehaviour.Properties.copy(Blocks.DARK_OAK_SAPLING)));

    public static final RegistryObject<Block> POPLAR_LOG = wood("poplar_log", "stripped_poplar_log");
    public static final RegistryObject<Block> STRIPPED_POPLAR_LOG = wood("stripped_poplar_log", null);
    public static final RegistryObject<Block> POPLAR_WOOD = wood("poplar_wood", "stripped_poplar_wood");
    public static final RegistryObject<Block> STRIPPED_POPLAR_WOOD = wood("stripped_poplar_wood", null);
    public static final RegistryObject<Block> POPLAR_PLANKS = planks("poplar_planks");
    public static final RegistryObject<Block> RED_POPLAR_LEAVES = leaves("red_poplar_leaves");
    public static final RegistryObject<Block> ORANGE_POPLAR_LEAVES = leaves("orange_poplar_leaves");
    public static final RegistryObject<Block> YELLOW_POPLAR_LEAVES = leaves("yellow_poplar_leaves");
    public static final RegistryObject<Block> POPLAR_SAPLING = add("poplar_sapling", () -> new SaplingBlock(new AbstractTreeGrower() {
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) {
            return ForestWorldgen.featureKey(new String[]{"red_poplar", "orange_poplar", "yellow_poplar"}[random.nextInt(3)] + "_bonemeal");
        }
    }, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));

    public static final RegistryObject<Block> PALE_MOSS_BLOCK = add("pale_moss_block", ForestPlants.PaleMoss::new);
    public static final RegistryObject<Block> PALE_MOSS_CARPET = add("pale_moss_carpet", com.y271727uy.farmerstales.gameplay.environment.forest.PaleMossCarpet::new);
    public static final RegistryObject<Block> PALE_HANGING_MOSS = add("pale_hanging_moss", ForestPlants.HangingMoss::new);
    public static final RegistryObject<Block> CLOSED_EYEBLOSSOM = add("closed_eyeblossom", () -> new ForestPlants.Eyeblossom(false, MobEffects.CONFUSION, 7));
    public static final RegistryObject<Block> OPEN_EYEBLOSSOM = add("open_eyeblossom", () -> new ForestPlants.Eyeblossom(true, MobEffects.BLINDNESS, 11));
    public static final RegistryObject<Block> RED_SHRUB = add("red_shrub", ForestPlants.RedShrub::new);
    public static final RegistryObject<Block> SHELF_MUSHROOM = add("shelf_mushroom", ForestPlants.ShelfMushroom::new);
    public static final RegistryObject<Block> LEAF_LITTER = add("leaf_litter", ForestPlants.LeafLitter::new);

    public static final Map<String, RegistryObject<Block>> BUILDING = new LinkedHashMap<>();
    static {
        for (String tree : new String[]{"pale_oak", "poplar"}) {
            building(tree, "stairs", () -> new StairBlock(() -> ALL.get(tree + "_planks").get().defaultBlockState(), BlockBehaviour.Properties.copy(Blocks.OAK_STAIRS)) {
                @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) ? 0 : 20; }
                @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return getFlammability(state, level, pos, face) == 0 ? 0 : 5; }
            });
            building(tree, "slab", () -> new SlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SLAB)) {
                @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) ? 0 : 20; }
                @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return getFlammability(state, level, pos, face) == 0 ? 0 : 5; }
            });
            building(tree, "fence", () -> new FenceBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE)) {
                @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) ? 0 : 20; }
                @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return getFlammability(state, level, pos, face) == 0 ? 0 : 5; }
            });
            building(tree, "fence_gate", () -> new FenceGateBlock(BlockBehaviour.Properties.copy(Blocks.OAK_FENCE_GATE), net.minecraft.world.level.block.state.properties.WoodType.OAK) {
                @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED) ? 0 : 20; }
                @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return getFlammability(state, level, pos, face) == 0 ? 0 : 5; }
            });
            building(tree, "door", () -> new DoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_DOOR), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
            building(tree, "trapdoor", () -> new TrapDoorBlock(BlockBehaviour.Properties.copy(Blocks.OAK_TRAPDOOR), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
            building(tree, "button", () -> new ButtonBlock(BlockBehaviour.Properties.copy(Blocks.OAK_BUTTON), net.minecraft.world.level.block.state.properties.BlockSetType.OAK, 30, true));
            building(tree, "pressure_plate", () -> new PressurePlateBlock(PressurePlateBlock.Sensitivity.EVERYTHING, BlockBehaviour.Properties.copy(Blocks.OAK_PRESSURE_PLATE), net.minecraft.world.level.block.state.properties.BlockSetType.OAK));
        }
    }
    private static void building(String tree, String part, Supplier<Block> factory) {
        String name = tree + "_" + part;
        BUILDING.put(name, add(name, factory));
    }

    private ForestBlocks() {}

    private static RegistryObject<Block> add(String name, Supplier<Block> factory) {
        var block = BLOCKS.register(name, factory);
        ALL.put(name, block);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) {
                if (name.equals("leaf_litter")) return 100;
                if (BUILDING.containsKey(name)) return name.endsWith("slab") ? 150 : name.endsWith("button") ? 100 : name.endsWith("door") && !name.endsWith("trapdoor") ? 200 : 300;
                if (name.contains("sapling")) return 100;
                if (name.contains("log") || name.contains("wood") || name.endsWith("planks")) return 300;
                return -1;
            }
        });
        return block;
    }

    private static RegistryObject<Block> wood(String name, String stripped) {
        return add(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG)) {
            @Override public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction action, boolean simulate) {
                if (stripped != null && action == ToolActions.AXE_STRIP && context.getItemInHand().canPerformAction(action)) {
                    return ALL.get(stripped).get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
                }
                return super.getToolModifiedState(state, context, action, simulate);
            }
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
        });
    }

    private static RegistryObject<Block> leaves(String name) {
        return add(name, () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.getValue(WATERLOGGED) ? 0 : 60; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return state.getValue(WATERLOGGED) ? 0 : 30; }
        });
    }

    private static RegistryObject<Block> planks(String name) {
        return add(name, () -> new Block(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)) {
            @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 20; }
            @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
        });
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ForestWorldgen.register(bus);
        ForestLeafParticles.register(bus);
        bus.addListener(ForestBlocks::commonSetup);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            ALL.keySet().forEach(name -> {
                var title = new StringBuilder();
                for (String word : name.split("_")) {
                    if (!title.isEmpty()) title.append(' ');
                    title.append(word.substring(0, 1).toUpperCase(Locale.ROOT)).append(word.substring(1));
                }
                provider.add("block.farmerstales." + name, title.toString());
            });
            provider.add("biome.farmerstales.pale_garden", "Pale Garden");
            provider.add("biome.farmerstales.dappled_forest", "Dappled Forest");
        });
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ALL.forEach((name, block) -> {
                if (name.contains("leaves") || name.contains("sapling") || name.contains("moss")
                        || name.contains("eyeblossom") || name.equals("red_shrub") || name.equals("leaf_litter") || name.equals("shelf_mushroom")) {
                    ComposterBlock.COMPOSTABLES.put(block.get().asItem(), name.contains("eyeblossom")
                            || name.equals("pale_moss_block") || name.equals("shelf_mushroom") ? 0.65F : 0.3F);
                }
            });
        });
    }
}

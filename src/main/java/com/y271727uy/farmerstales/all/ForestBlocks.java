package com.y271727uy.farmerstales.all;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.backport.Backport;
import com.y271727uy.farmerstales.backport.BackportFeature;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestPlants;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/**
 * Natural blocks and renewable tree materials of the Dappled Forest (poplar). The back-ported
 * Pale Garden blocks live in {@link PaleGardenBlocks}; leaf litter lives in
 * {@link NaturalEnvironmentBlocks}.
 */
@Backport(BackportFeature.POPLAR)
public final class ForestBlocks {
    private static final WoodBlockRegistrar REGISTRAR = new WoodBlockRegistrar();
    public static final Map<String, RegistryObject<Block>> ALL = REGISTRAR.all;
    public static final Map<String, RegistryObject<Block>> BUILDING = REGISTRAR.building;

    public static final RegistryObject<Block> POPLAR_LOG = REGISTRAR.wood("poplar_log", "stripped_poplar_log");
    public static final RegistryObject<Block> STRIPPED_POPLAR_LOG = REGISTRAR.wood("stripped_poplar_log", null);
    public static final RegistryObject<Block> POPLAR_WOOD = REGISTRAR.wood("poplar_wood", "stripped_poplar_wood");
    public static final RegistryObject<Block> STRIPPED_POPLAR_WOOD = REGISTRAR.wood("stripped_poplar_wood", null);
    public static final RegistryObject<Block> POPLAR_PLANKS = REGISTRAR.planks("poplar_planks");
    public static final RegistryObject<Block> RED_POPLAR_LEAVES = REGISTRAR.leaves("red_poplar_leaves");
    public static final RegistryObject<Block> ORANGE_POPLAR_LEAVES = REGISTRAR.leaves("orange_poplar_leaves");
    public static final RegistryObject<Block> YELLOW_POPLAR_LEAVES = REGISTRAR.leaves("yellow_poplar_leaves");
    public static final RegistryObject<Block> POPLAR_SAPLING = REGISTRAR.add("poplar_sapling", () -> new SaplingBlock(new AbstractTreeGrower() {
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) {
            return ForestWorldgen.featureKey(new String[]{"red_poplar", "orange_poplar", "yellow_poplar"}[random.nextInt(3)] + "_bonemeal");
        }
    }, BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));
    public static final RegistryObject<Block> RED_SHRUB = REGISTRAR.add("red_shrub", ForestPlants.RedShrub::new);
    public static final RegistryObject<Block> SHELF_MUSHROOM = REGISTRAR.add("shelf_mushroom", ForestPlants.ShelfMushroom::new);
    public static final RegistryObject<Block> POTTED_POPLAR_SAPLING = REGISTRAR.pot("poplar_sapling",
            () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, POPLAR_SAPLING, BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY)));

    static {
        REGISTRAR.buildingSet("poplar");
    }

    private ForestBlocks() {}

    /** Planks of a forest wood ({@code pale_oak} or {@code poplar}), or air when that wood is disabled. */
    public static Block planks(String wood) {
        var block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, wood + "_planks"));
        return block == null ? Blocks.AIR : block;
    }

    public static void register(IEventBus bus) {
        REGISTRAR.register(bus, Map.of("biome.farmerstales.dappled_forest", "Dappled Forest"));
    }
}

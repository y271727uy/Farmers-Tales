package com.y271727uy.farmerstales.all;

import com.y271727uy.farmerstales.backport.Backport;
import com.y271727uy.farmerstales.backport.BackportFeature;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestPlants;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestWorldgen;
import com.y271727uy.farmerstales.gameplay.environment.forest.PaleMossCarpet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.grower.AbstractMegaTreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/** The Garden Awakens (1.21.4): pale oak wood set, pale moss and eyeblossoms. */
@Backport(BackportFeature.PALE_GARDEN)
public final class PaleGardenBlocks {
    private static final WoodBlockRegistrar REGISTRAR = new WoodBlockRegistrar();
    public static final Map<String, RegistryObject<Block>> ALL = REGISTRAR.all;
    public static final Map<String, RegistryObject<Block>> BUILDING = REGISTRAR.building;

    public static final RegistryObject<Block> PALE_OAK_LOG = REGISTRAR.wood("pale_oak_log", "stripped_pale_oak_log");
    public static final RegistryObject<Block> STRIPPED_PALE_OAK_LOG = REGISTRAR.wood("stripped_pale_oak_log", null);
    public static final RegistryObject<Block> PALE_OAK_WOOD = REGISTRAR.wood("pale_oak_wood", "stripped_pale_oak_wood");
    public static final RegistryObject<Block> STRIPPED_PALE_OAK_WOOD = REGISTRAR.wood("stripped_pale_oak_wood", null);
    public static final RegistryObject<Block> PALE_OAK_PLANKS = REGISTRAR.planks("pale_oak_planks");
    public static final RegistryObject<Block> PALE_OAK_LEAVES = REGISTRAR.leaves("pale_oak_leaves");
    public static final RegistryObject<Block> PALE_OAK_SAPLING = REGISTRAR.add("pale_oak_sapling", () -> new SaplingBlock(new AbstractMegaTreeGrower() {
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean flowers) { return null; }
        @Override protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> getConfiguredMegaFeature(RandomSource random) { return ForestWorldgen.featureKey("pale_oak_bonemeal"); }
    }, BlockBehaviour.Properties.copy(Blocks.DARK_OAK_SAPLING)));

    public static final RegistryObject<Block> PALE_MOSS_BLOCK = REGISTRAR.add("pale_moss_block", ForestPlants.PaleMoss::new);
    public static final RegistryObject<Block> PALE_MOSS_CARPET = REGISTRAR.add("pale_moss_carpet", PaleMossCarpet::new);
    public static final RegistryObject<Block> PALE_HANGING_MOSS = REGISTRAR.add("pale_hanging_moss", ForestPlants.HangingMoss::new);
    public static final RegistryObject<Block> CLOSED_EYEBLOSSOM = REGISTRAR.add("closed_eyeblossom", () -> new ForestPlants.Eyeblossom(false, MobEffects.CONFUSION, 7));
    public static final RegistryObject<Block> OPEN_EYEBLOSSOM = REGISTRAR.add("open_eyeblossom", () -> new ForestPlants.Eyeblossom(true, MobEffects.BLINDNESS, 11));

    public static final RegistryObject<Block> POTTED_PALE_OAK_SAPLING = pot("pale_oak_sapling", PALE_OAK_SAPLING);
    public static final RegistryObject<Block> POTTED_OPEN_EYEBLOSSOM = pot("open_eyeblossom", OPEN_EYEBLOSSOM);
    public static final RegistryObject<Block> POTTED_CLOSED_EYEBLOSSOM = pot("closed_eyeblossom", CLOSED_EYEBLOSSOM);

    static {
        REGISTRAR.buildingSet("pale_oak");
    }

    private PaleGardenBlocks() {}

    /** Potted eyeblossoms open and close with the day cycle like planted ones. */
    private static RegistryObject<Block> pot(String plant, RegistryObject<Block> content) {
        return REGISTRAR.pot(plant, () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, content,
                BlockBehaviour.Properties.copy(Blocks.POTTED_POPPY).randomTicks()) {
            @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
                if (plant.contains("eyeblossom") && level.dimensionType().natural() && level.isNight() != plant.startsWith("open")) {
                    level.setBlockAndUpdate(pos, (level.isNight() ? POTTED_OPEN_EYEBLOSSOM : POTTED_CLOSED_EYEBLOSSOM).get().defaultBlockState());
                }
            }
        });
    }

    /** Called by BackportLoader when {@link BackportFeature#PALE_GARDEN} is enabled. */
    public static void register(IEventBus bus) {
        REGISTRAR.register(bus, Map.of("biome.farmerstales.pale_garden", "Pale Garden"));
    }
}

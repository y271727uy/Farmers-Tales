package com.y271727uy.farmerstales.all;

import com.tterrag.registrate.providers.ProviderType;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import com.y271727uy.farmerstales.gameplay.environment.SpringPlants;
import com.y271727uy.farmerstales.gameplay.environment.SpringNatureEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.IForgeShearable;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Supplier;

/** Small plants from Spring to Life, adapted to the 1.20.1 plant APIs. */
public final class NaturalEnvironmentBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, FTMod.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID);
    public static final RegistryObject<Block> SHORT_DRY_GRASS = plant("short_dry_grass", Kind.SHORT_DRY);
    public static final RegistryObject<Block> TALL_DRY_GRASS = plant("tall_dry_grass", Kind.TALL_DRY);
    public static final RegistryObject<Block> BUSH = plant("bush", Kind.BUSH);
    public static final RegistryObject<Block> FIREFLY_BUSH = simplePlant("firefly_bush", SpringPlants.FireflyBush::new);
    public static final RegistryObject<Block> WILDFLOWERS = simplePlant("wildflowers", () -> new PinkPetalsBlock(BlockBehaviour.Properties.copy(Blocks.PINK_PETALS)));
    public static final RegistryObject<Block> CACTUS_FLOWER = simplePlant("cactus_flower", SpringPlants.CactusFlower::new);

    private NaturalEnvironmentBlocks() {}

    private enum Kind { SHORT_DRY, TALL_DRY, BUSH }

    private static RegistryObject<Block> simplePlant(String name, Supplier<Block> factory) {
        var block = BLOCKS.register(name, factory);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    private static RegistryObject<Block> plant(String name, Kind kind) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new EnvironmentPlant(kind));
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()) {
            @Override
            public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
                return kind == Kind.BUSH ? -1 : 100;
            }
        });
        return block;
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SpringNatureEffects.register(bus);
        bus.addListener(NaturalEnvironmentBlocks::commonSetup);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            provider.add("block.farmerstales.short_dry_grass", "Short Dry Grass");
            provider.add("block.farmerstales.tall_dry_grass", "Tall Dry Grass");
            provider.add("block.farmerstales.bush", "Bush");
            provider.add("block.farmerstales.firefly_bush", "Firefly Bush");
            provider.add("block.farmerstales.wildflowers", "Wildflowers");
            provider.add("block.farmerstales.cactus_flower", "Cactus Flower");
            provider.add("subtitles.farmerstales.block.firefly_bush.idle", "Fireflies buzz");
        });
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ComposterBlock.COMPOSTABLES.put(SHORT_DRY_GRASS.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(TALL_DRY_GRASS.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(BUSH.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(FIREFLY_BUSH.get().asItem(), 0.3F);
            ComposterBlock.COMPOSTABLES.put(WILDFLOWERS.get().asItem(), 0.65F);
            ComposterBlock.COMPOSTABLES.put(CACTUS_FLOWER.get().asItem(), 0.3F);
        });
    }

    public static boolean isDryGrass(BlockState state) {
        return state.is(SHORT_DRY_GRASS.get()) || state.is(TALL_DRY_GRASS.get());
    }

    private static final class EnvironmentPlant extends BushBlock implements BonemealableBlock, IForgeShearable {
        private final Kind kind;

        private EnvironmentPlant(Kind kind) {
            super(BlockBehaviour.Properties.copy(kind == Kind.BUSH ? Blocks.GRASS : Blocks.DEAD_BUSH)
                    .noCollission().instabreak().replaceable().offsetType(BlockBehaviour.OffsetType.XZ));
            this.kind = kind;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            var offset = state.getOffset(level, pos);
            return Block.box(2, 0, 2, 14, kind == Kind.TALL_DRY ? 13 : 10, 14)
                    .move(offset.x, offset.y, offset.z);
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return kind == Kind.BUSH ? super.mayPlaceOn(state, level, pos)
                    : state.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON) || state.is(Blocks.FARMLAND);
        }

        private BlockState spreadState() {
            return (kind == Kind.BUSH ? BUSH : SHORT_DRY_GRASS).get().defaultBlockState();
        }

        @Override
        public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) {
            if (kind == Kind.SHORT_DRY) return true;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos target = pos.relative(direction);
                if (level.isEmptyBlock(target) && spreadState().canSurvive(level, target)) return true;
            }
            return false;
        }

        @Override
        public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
            return true;
        }

        @Override
        public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
            if (kind == Kind.SHORT_DRY) {
                level.setBlockAndUpdate(pos, TALL_DRY_GRASS.get().defaultBlockState());
                return;
            }
            // Randomize the first direction, then try every adjacent position once.
            int start = random.nextInt(4);
            for (int i = 0; i < 4; i++) {
                BlockPos target = pos.relative(Direction.from2DDataValue((start + i) % 4));
                BlockState plant = spreadState();
                if (level.isEmptyBlock(target) && plant.canSurvive(level, target)) {
                    level.setBlockAndUpdate(target, plant);
                    return;
                }
            }
        }

        @Override
        public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 100;
        }

        @Override
        public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
            return 60;
        }
    }
}

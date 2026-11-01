package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ModBlockEntities;
import com.y271727uy.farmerstales.all.ModBlocks;
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity;
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropEvents;
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropParents;
import forestry.api.genetics.alleles.ForestryAlleles;
import forestry.api.IForestryApi;
import com.y271727uy.farmerstales.all.items.CropPollenItem;
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions;
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenSample;
import com.y271727uy.farmerstales.integration.forestry.pollen.CropPollenType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Run using the project's existing runGameTestServer configuration. */
@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class CropStandGameTests {
    @GameTest(template = "crop_stand_empty", setupTicks = 20, timeoutTicks = 200)
    public static void forestryApiaryWorkCyclePollinatesCropStands(GameTestHelper helper) {
        var hivePos = new BlockPos(2, 1, 2);
        var apiaryBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", "apiary"));
        helper.assertTrue(apiaryBlock != Blocks.AIR, "Forestry apiary block must exist");
        helper.setBlock(hivePos, apiaryBlock);
        var apiary = (forestry.apiculture.tiles.TileApiary) helper.getBlockEntity(hivePos);
        // Forestry's own test frame removes climate/flower/time constraints and speeds up pollen.
        // The queen and its normal work cycle still collect and transfer pollen themselves.
        var frame = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("forestry", "frame_creative"));
        helper.assertTrue(frame != Items.AIR, "Forestry creative test frame must exist");
        apiary.setItem(forestry.apiculture.inventory.InventoryApiary.SLOT_FRAMES_1, new ItemStack(frame));
        var bees = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(forestry.api.genetics.ForestrySpeciesTypes.BEE,
                forestry.api.apiculture.genetics.IBeeSpeciesType.class);
        var species = bees.getDefaultSpecies();
        var genome = species.getDefaultGenome().copyWith(java.util.Map.of(
                forestry.api.genetics.alleles.BeeChromosomes.LIFESPAN, ForestryAlleles.LIFESPAN_IMMORTAL,
                forestry.api.genetics.alleles.BeeChromosomes.POLLINATION, ForestryAlleles.POLLINATION_MAXIMUM));
        var queen = species.createIndividual(genome);
        queen.setMate(genome);
        apiary.getBeeInventory().setQueen(queen.createStack(forestry.api.apiculture.genetics.BeeLifeStage.QUEEN));
        var stands = new java.util.ArrayList<CropStandBlockEntity>();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            if (x == 2 && z == 2) continue;
            var crop = stand(helper, new BlockPos(x, 1, z));
            crop.plant(genePotato(helper, (x + z) % ForestryAlleles.DEFAULT_MATURATIONS.size()), false);
            crop.grow(100);
            stands.add(crop);
        }
        var logic = apiary.getBeekeepingLogic();
        logic.setWorkThrottle(1);
        helper.assertTrue(logic.canWork(), "Queen must be able to work in the test apiary");
        // A bounded number of genuine Forestry work cycles; no direct adapter calls or reflection.
        for (int cycle = 0; cycle < 2000 && stands.stream().noneMatch(s -> s.pollenFather() != null); cycle++) {
            logic.doWork();
            // Production must not fill storage and interfere with the pollen integration check.
            for (int slot = forestry.apiculture.InventoryBeeHousing.SLOT_PRODUCT_1;
                 slot < forestry.apiculture.InventoryBeeHousing.SLOT_PRODUCT_1 + forestry.apiculture.InventoryBeeHousing.SLOT_PRODUCT_COUNT; slot++) {
                apiary.setItem(slot, ItemStack.EMPTY);
            }
        }
        var receiver = stands.stream().filter(s -> s.pollenFather() != null).findFirst();
        helper.assertTrue(receiver.isPresent(), "Working Forestry queen must collect and transfer crop pollen");
        var crop = receiver.orElseThrow();
        var father = GeneCropGenome.stored(crop.pollenFather());
        helper.assertTrue(father != null, "Bee-carried pollen must retain the donor genome");
        helper.assertTrue(crop.harvest(helper.makeMockPlayer()), "Bee-pollinated crop must harvest normally");
        helper.assertTrue(crop.pollenFather() == null, "Harvest must clear bee pollination state");
        helper.succeed();
    }

    private static ItemStack genePotato(GameTestHelper helper, int alleleIndex) {
        var seed = new ItemStack(Items.POTATO);
        GeneCropGenome.identify(seed, helper.getLevel().random);
        var alleleId = ForestryAlleles.DEFAULT_MATURATIONS.get(alleleIndex).alleleId().toString();
        var pair = new CompoundTag();
        pair.putString("active", alleleId);
        pair.putString("inactive", alleleId);
        seed.getOrCreateTag().getCompound(GeneCropGenome.TAG).put(GeneCropGenome.GROWTH, pair);
        return seed;
    }

    private static CropStandBlockEntity stand(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos, ModBlocks.CROP_STAND.get());
        return (CropStandBlockEntity) helper.getBlockEntity(pos);
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void plantsVanillaSeedsAndRejectsOverwrite(GameTestHelper helper) {
        var seeds = new net.minecraft.world.item.Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS};
        var crops = new net.minecraft.world.level.block.Block[]{Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};
        for (int i = 0; i < seeds.length; i++) {
            var stand = stand(helper, new BlockPos(i, 1, 1));
            var stack = new ItemStack(seeds[i], 2);
            helper.assertTrue(stand.plant(stack, false), "Vanilla seed must plant: " + seeds[i]
                    + "; item=" + seeds[i].getClass().getName()
                    + "; crop=" + crops[i].getClass().getName()
                    + "; soil=" + helper.getLevel().getBlockState(stand.getBlockPos().below())
                    + "; light=" + helper.getLevel().getRawBrightness(stand.getBlockPos(), 0)
                    + "; survives=" + crops[i].defaultBlockState().canSurvive(helper.getLevel(), stand.getBlockPos()));
            helper.assertTrue(stack.getCount() == 1, "Planting must consume exactly one seed");
            helper.assertTrue(stand.getCropState().is(crops[i]), "Seed must select its corresponding crop");
            helper.assertFalse(stand.plant(stack, false), "Occupied stand must reject another seed");
            helper.assertTrue(stack.getCount() == 1, "Rejected planting must not consume a seed");
        }
        var empty = stand(helper, new BlockPos(1, 1, 3));
        helper.assertFalse(empty.plant(new ItemStack(Items.DIRT), false), "Dirt must not plant as a crop");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void savesGrowthAndSeedMetadata(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var stand = stand(helper, pos);
        var seed = new ItemStack(Items.POTATO, 3);
        seed.getOrCreateTag().putString("CropStandTest", "retained");
        stand.plant(seed, false);
        stand.grow(3);
        var saved = stand.saveWithFullMetadata();
        var loaded = new CropStandBlockEntity(ModBlockEntities.CROP_STAND.get(), helper.absolutePos(pos), stand.getBlockState());
        loaded.load(saved);
        helper.assertTrue(loaded.getCropState().equals(stand.getCropState()), "Reload must preserve crop age");
        var restored = ItemStack.of(loaded.getUpdateTag().getCompound("Seed"));
        helper.assertTrue(restored.getCount() == 1, "Stored seed must have count one");
        helper.assertTrue("retained".equals(restored.getOrCreateTag().getString("CropStandTest")), "Reload and client sync must preserve seed NBT");
        loaded.load(new CompoundTag());
        helper.assertTrue(loaded.isEmpty(), "Loading empty data must clear old contents");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void harvestReplantsAndRemovalReturnsSeedOnce(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var stand = stand(helper, pos);
        stand.plant(new ItemStack(Items.POTATO), false);
        helper.assertFalse(stand.harvest(helper.makeMockPlayer()), "Immature crop must not harvest");
        stand.grow(100);
        helper.assertTrue(stand.harvest(helper.makeMockPlayer()), "Mature crop must harvest");
        helper.assertFalse(stand.isEmpty(), "Potato harvest must retain one potato for replanting");
        helper.assertFalse(stand.isMature(), "Harvested crop must return to its initial age");
        helper.assertFalse(stand.harvest(helper.makeMockPlayer()), "Repeated harvest must not duplicate loot");
        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.setBlock(pos, Blocks.AIR);
        var items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1));
        int potatoes = items.stream().filter(e -> e.getItem().is(Items.POTATO)).mapToInt(e -> e.getItem().getCount()).sum();
        helper.assertTrue(potatoes == 1, "Removing the stand must return exactly one retained seed");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void ordinaryFieldsIgnoreLegacyGenetics(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var absolute = helper.absolutePos(pos);
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        var mature = ((net.minecraft.world.level.block.CropBlock) Blocks.POTATOES).getStateForAge(7);
        helper.setBlock(pos, mature);
        var legacy = GeneCropParents.get(helper.getLevel());
        legacy.plant(helper.getLevel(), absolute, genePotato(helper, 0));
        helper.assertTrue(GeneCropEvents.prepare(helper.getLevel(), absolute, mature) == null, "Ordinary crops must not inherit, even with old saved parent data");
        helper.assertFalse(GeneCropGenome.isGeneCrop(mature, helper.getLevel(), absolute), "Ordinary crops must not expose planted genetics");
        mature.getBlock().playerDestroy(helper.getLevel(), helper.makeMockPlayer(), absolute, mature, null, ItemStack.EMPTY);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(absolute).inflate(1));
        helper.assertTrue(!drops.isEmpty(), "Ordinary harvest must still produce drops");
        helper.assertTrue(drops.stream().noneMatch(e -> GeneCropGenome.isIdentified(e.getItem())), "Ordinary harvest must not add genomes to drops");
        legacy.clear(helper.getLevel(), absolute);
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void matureStandsCrossAndKeepOriginalParent(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var mother = stand(helper, pos);
        var father = stand(helper, pos.east());
        var maternal = genePotato(helper, 0);
        var paternal = genePotato(helper, ForestryAlleles.DEFAULT_MATURATIONS.size() - 1);
        var motherGenome = GeneCropGenome.stored(maternal);
        var first = motherGenome.getCompound(GeneCropGenome.GROWTH).getString("active");
        var second = GeneCropGenome.stored(paternal).getCompound(GeneCropGenome.GROWTH).getString("active");
        helper.assertFalse(first.equals(second), "Test parents must have distinct alleles");
        mother.plant(maternal, false);
        father.plant(paternal, false);
        mother.grow(100);
        father.grow(100);
        helper.assertTrue(mother.harvest(helper.makeMockPlayer()), "Mature stand must harvest");
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1));
        var seeds = drops.stream().filter(e -> e.getItem().is(Items.POTATO)).toList();
        helper.assertTrue(!seeds.isEmpty(), "Cross must produce surplus seed drops");
        for (var entity : seeds) {
            var genome = GeneCropGenome.stored(entity.getItem());
            helper.assertTrue(genome != null, "Offspring must carry a genome");
            var pair = genome.getCompound(GeneCropGenome.GROWTH);
            var active = pair.getString("active");
            var inactive = pair.getString("inactive");
            helper.assertTrue((first.equals(active) && second.equals(inactive)) || (second.equals(active) && first.equals(inactive)), "Child must inherit one allele from each stand");
        }
        helper.assertTrue(motherGenome.equals(GeneCropGenome.stored(mother.breedingSeed())), "Replanting must keep the original parent genome");
        helper.assertFalse(mother.isMature(), "Harvest must reset growth");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void fieldsAndImmatureStandsCannotSupplyPollen(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var mother = stand(helper, pos);
        var maternal = genePotato(helper, 0);
        var expected = GeneCropGenome.stored(maternal).getCompound(GeneCropGenome.GROWTH);
        mother.plant(maternal, false);
        mother.grow(100);
        var immature = stand(helper, pos.east());
        immature.plant(genePotato(helper, ForestryAlleles.DEFAULT_MATURATIONS.size() - 1), false);
        var fieldPos = pos.south();
        helper.setBlock(fieldPos.below(), Blocks.FARMLAND);
        helper.setBlock(fieldPos, ((net.minecraft.world.level.block.CropBlock) Blocks.POTATOES).getStateForAge(7));
        var legacy = GeneCropParents.get(helper.getLevel());
        legacy.plant(helper.getLevel(), helper.absolutePos(fieldPos), genePotato(helper, ForestryAlleles.DEFAULT_MATURATIONS.size() - 1));
        var child = GeneCropEvents.prepare(helper.getLevel(), helper.absolutePos(pos));
        helper.assertTrue(child != null && expected.equals(child.getCompound(GeneCropGenome.GROWTH)), "Field and immature neighbors must be ignored; isolated parent self-pollinates");
        legacy.clear(helper.getLevel(), helper.absolutePos(fieldPos));
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void unidentifiedStandSeedsDoNotAcquireGenomes(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var mother = stand(helper, pos);
        var father = stand(helper, pos.east());
        mother.plant(new ItemStack(Items.POTATO), false);
        father.plant(genePotato(helper, 0), false);
        mother.grow(100);
        father.grow(100);
        helper.assertTrue(GeneCropEvents.prepare(helper.getLevel(), helper.absolutePos(pos)) == null, "Plain planted seed must not be assigned random genetics");
        mother.harvest(helper.makeMockPlayer());
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(1));
        helper.assertTrue(drops.stream().noneMatch(e -> GeneCropGenome.isIdentified(e.getItem())), "Plain seed harvest must remain plain");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void forestryPollenStoresAndUsesDonorOnce(GameTestHelper helper) {
        var manager = IForestryApi.INSTANCE.getPollenManager();
        helper.assertTrue(manager.getPollenType(ResourceLocation.fromNamespaceAndPath("farmerstales", "crop")) == CropPollenType.INSTANCE, "Forestry must register the shared FT pollen adapter");
        var donorPos = new BlockPos(0, 1, 0);
        var receiverPos = new BlockPos(3, 1, 3);
        var donor = stand(helper, donorPos);
        var receiver = stand(helper, receiverPos);
        donor.plant(genePotato(helper, ForestryAlleles.DEFAULT_MATURATIONS.size() - 1), false);
        receiver.plant(genePotato(helper, 0), false);
        donor.grow(100);
        receiver.grow(100);
        var maternalGrowth = GeneCropGenome.stored(receiver.breedingSeed()).getCompound(GeneCropGenome.GROWTH).getString("active");
        var paternalGrowth = GeneCropGenome.stored(donor.breedingSeed()).getCompound(GeneCropGenome.GROWTH).getString("active");
        var pollen = manager.getPollen(helper.getLevel(), helper.absolutePos(donorPos), null);
        helper.assertTrue(pollen != null && pollen.getType() == CropPollenType.INSTANCE, "Forestry manager must collect crop pollen");
        var restored = CropPollenType.INSTANCE.readNbt(pollen.writeNbt());
        var item = restored.createStack();
        helper.assertTrue(item.getItem() instanceof CropPollenItem && CropPollenItem.sample(item).isViable(), "Pollen must survive NBT and item conversion");
        helper.assertFalse(restored.tryPollinate(helper.getLevel(), helper.absolutePos(donorPos), null), "A pollen sample must not pollinate its source stand");
        helper.assertTrue(restored.tryPollinate(helper.getLevel(), helper.absolutePos(receiverPos), null), "Pollen must transfer to a distant compatible stand");
        helper.assertFalse(restored.tryPollinate(helper.getLevel(), helper.absolutePos(receiverPos), null), "A second visitor must not overwrite the recorded father");
        var loaded = new CropStandBlockEntity(ModBlockEntities.CROP_STAND.get(), receiver.getBlockPos(), receiver.getBlockState());
        loaded.load(receiver.saveWithFullMetadata());
        helper.assertTrue(GeneCropGenome.stored(loaded.pollenFather()).equals(GeneCropGenome.stored(donor.breedingSeed())), "The donor snapshot must survive a world save");
        var child = GeneCropEvents.prepare(helper.getLevel(), helper.absolutePos(receiverPos));
        var pair = child.getCompound(GeneCropGenome.GROWTH);
        helper.assertTrue((maternalGrowth.equals(pair.getString("active")) && paternalGrowth.equals(pair.getString("inactive"))) ||
                (paternalGrowth.equals(pair.getString("active")) && maternalGrowth.equals(pair.getString("inactive"))), "Recorded pollen must supply the father even outside neighbor range");
        receiver.harvest(helper.makeMockPlayer());
        helper.assertTrue(receiver.pollenFather() == null, "Harvest must clear the pollen for the next cycle");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void pollenRejectsFieldsImmatureAndDisplaySamples(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var receiver = stand(helper, pos);
        receiver.plant(genePotato(helper, 0), false);
        var donorSeed = genePotato(helper, 0);
        var pollen = CropPollenType.INSTANCE.readNbt(new CropPollenSample(donorSeed, null, null).write());
        helper.assertFalse(pollen.tryPollinate(helper.getLevel(), helper.absolutePos(pos), null), "Immature stands must reject pollen");
        receiver.grow(100);
        var display = new CropPollenSample(new ItemStack(Items.POTATO), null, null);
        helper.assertFalse(CropPollenType.INSTANCE.tryPollinate(helper.getLevel(), helper.absolutePos(pos), display, null), "Creative display samples must not fabricate donor genomes");
        helper.assertTrue(CropPollenType.INSTANCE.readNbt(new CompoundTag()).createStack().isEmpty(), "Malformed pollen must not create usable items");
        var carrot = stand(helper, pos.east());
        carrot.plant(new ItemStack(Items.CARROT), false);
        carrot.grow(100);
        helper.assertFalse(pollen.tryPollinate(helper.getLevel(), helper.absolutePos(pos.east()), null), "Other crop species must reject potato pollen");
        var field = pos.south();
        helper.setBlock(field.below(), Blocks.FARMLAND);
        helper.setBlock(field, ((net.minecraft.world.level.block.CropBlock) Blocks.POTATOES).getStateForAge(7));
        helper.assertFalse(pollen.tryPollinate(helper.getLevel(), helper.absolutePos(field), null), "Ordinary farmland must reject pollen");
        helper.assertTrue(CropPollenType.INSTANCE.tryCollectPollen(helper.getLevel(), helper.absolutePos(field), null) == null, "Ordinary farmland must not supply pollen");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void pollenItemConsumesOnlyOnSuccessfulPollination(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var receiver = stand(helper, pos);
        receiver.plant(genePotato(helper, 0), false);
        receiver.grow(100);
        var item = CropPollenItem.create(new CropPollenSample(genePotato(helper, 0), null, null));
        item.setCount(2);
        var player = helper.makeMockPlayer();
        player.getAbilities().instabuild = false;
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        var absolute = helper.absolutePos(pos);
        var context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
        helper.assertTrue(item.useOn(context).consumesAction() && item.getCount() == 1, "Hand pollination must consume exactly one sample");
        item.useOn(context);
        helper.assertTrue(item.getCount() == 1, "Rejected repeat pollination must not consume another sample");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void pollenDefinitionsReadLiveSeedTags(GameTestHelper helper) {
        var holder = BuiltInRegistries.ITEM.getHolderOrThrow(ResourceKey.create(Registries.ITEM, BuiltInRegistries.ITEM.getKey(Items.CARROT)));
        var original = holder.tags().toList();
        try {
            var tags = new java.util.ArrayList<>(original);
            tags.add(GeneCropGenome.SEEDS);
            holder.bindTags(tags);
            var seed = new ItemStack(Items.CARROT);
            helper.assertTrue(CropPollenDefinitions.INSTANCE.supports(seed), "DSL must admit crops added through the seed tag");
            helper.assertTrue(CropPollenDefinitions.INSTANCE.displaySeeds().stream().anyMatch(s -> s.is(Items.CARROT)), "Newly tagged crops must get a creative pollen variant");
            var receiver = stand(helper, new BlockPos(1, 1, 1));
            GeneCropGenome.identify(seed, helper.getLevel().random);
            receiver.plant(seed, false);
            receiver.grow(100);
            var pollen = CropPollenType.INSTANCE.tryCollectPollen(helper.getLevel(), receiver.getBlockPos(), null);
            helper.assertTrue(pollen != null && CropPollenItem.sample(pollen.createStack()).seed().is(Items.CARROT), "Tagged carrot must supply its own pollen without a dedicated item registration");
        } finally {
            holder.bindTags(original);
        }
        helper.succeed();
    }
}

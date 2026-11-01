package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ModBlocks;
import com.y271727uy.farmerstales.gameplay.gene_crop.CropTemperatureStatus;
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity;
import com.y271727uy.farmerstales.integration.forestry.genome.CropTemperature;
import forestry.api.IForestryApi;
import forestry.api.core.TemperatureType;
import forestry.api.core.ToleranceType;
import forestry.api.genetics.alleles.ForestryAlleles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.HashMap;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class CropTemperatureGameTests {
    @GameTest(template = "crop_stand_empty")
    public static void directionalToleranceMatchesForestry(GameTestHelper helper) {
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.NORMAL, ToleranceType.NONE) == CropTemperatureStatus.SUITABLE, "Normal climate must allow growth without extra tolerance");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.ICY, ToleranceType.DOWN_2) == CropTemperatureStatus.SUITABLE, "Two tiers of cold tolerance must allow icy climates");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.HOT, ToleranceType.UP_2) == CropTemperatureStatus.SUITABLE, "Two tiers of heat tolerance must allow hot climates");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.COLD, ToleranceType.NONE) == CropTemperatureStatus.CHILLY, "One cold tier must slow growth");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.ICY, ToleranceType.NONE) == CropTemperatureStatus.TOO_COLD, "Two cold tiers must stop growth");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.HOT, ToleranceType.NONE) == CropTemperatureStatus.TOO_HOT, "Two hot tiers must stop growth");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.WARM, ToleranceType.NONE) == CropTemperatureStatus.WARM, "One warm tier must slow growth");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.ICY, ToleranceType.UP_2) == CropTemperatureStatus.TOO_COLD, "Heat tolerance must not grant cold tolerance");
        helper.assertTrue(CropTemperature.evaluate(TemperatureType.HOT, ToleranceType.DOWN_2) == CropTemperatureStatus.TOO_HOT, "Cold tolerance must not grant heat tolerance");
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void cropClimateControlsGrowthAndBonemeal(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(2, 1, 2);
        var absolute = helper.absolutePos(pos);
        var icy = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.SNOWY_PLAINS);
        var sampler = level.getChunkSource().randomState().sampler();
        // Snapshot the nearby noise-biome cells. Restore them before another test
        // runs, including cells sampled across chunk boundaries by BiomeManager.
        var originals = new HashMap<net.minecraft.world.level.chunk.LevelChunk, HashMap<BlockPos, Holder<Biome>>>();
        try {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var chunk = level.getChunkAt(absolute.offset(dx * 16, 0, dz * 16));
                var cells = new HashMap<BlockPos, Holder<Biome>>();
                int qx = chunk.getPos().x * 4, qz = chunk.getPos().z * 4;
                for (int x = qx; x < qx + 4; x++) for (int z = qz; z < qz + 4; z++)
                    for (int y = level.getMinBuildHeight() >> 2; y < level.getMaxBuildHeight() >> 2; y++)
                        cells.put(new BlockPos(x, y, z), chunk.getNoiseBiome(x, y, z));
                originals.put(chunk, cells);
                chunk.fillBiomesFromNoise((x, y, z, noise) -> icy, sampler);
            }
            helper.assertTrue(IForestryApi.INSTANCE.getClimateManager().getState(level, absolute).temperature() == TemperatureType.ICY,
                    "Test must use the crop position's actual Forestry climate");

            var stopped = stand(helper, pos, geneSeed(helper, ToleranceType.NONE, ToleranceType.DOWN_2));
            helper.assertTrue(stopped.temperatureStatus() == CropTemperatureStatus.TOO_COLD, "Inactive cold tolerance must not be expressed");
            for (int i = 0; i < 300; i++) stopped.growNaturally(level, level.random);
            stopped.grow(4);
            stopped.fertilize(4);
            helper.assertTrue(age(stopped) == 0 && !stopped.canFertilize(), "Cold must stop natural growth, direct growth, and bone meal");

            var tolerant = stand(helper, pos.east(), geneSeed(helper, ToleranceType.DOWN_2, ToleranceType.NONE));
            helper.assertTrue(tolerant.temperatureStatus() == CropTemperatureStatus.SUITABLE, "Active cold tolerance must enable growth");
            tolerant.fertilize(4);
            helper.assertTrue(age(tolerant) == 4, "Suitable plants must keep full bone-meal growth");

            var slowed = stand(helper, pos.west(), geneSeed(helper, ToleranceType.DOWN_1, ToleranceType.NONE));
            helper.assertTrue(slowed.temperatureStatus() == CropTemperatureStatus.CHILLY, "One tier outside tolerance must slow growth");
            slowed.fertilize(4);
            helper.assertTrue(age(slowed) == 2, "Marginal climate must halve bone-meal growth");

            var plain = stand(helper, pos.south(), new ItemStack(Items.POTATO));
            plain.fertilize(4);
            helper.assertTrue(age(plain) == 4, "Unidentified ordinary seeds must retain existing growth behavior");

            var malformed = geneSeed(helper, ToleranceType.NONE, ToleranceType.NONE);
            malformed.getOrCreateTag().getCompound(GeneCropGenome.TAG).getCompound(GeneCropGenome.TEMPERATURE)
                    .putString("active", "unknown:missing_allele");
            helper.assertTrue(GeneCropGenome.temperatureTolerance(malformed) == ToleranceType.NONE, "Missing allele must safely fall back to no tolerance");
            helper.succeed();
        } finally {
            originals.forEach((chunk, cells) -> chunk.fillBiomesFromNoise((x, y, z, noise) -> cells.get(new BlockPos(x, y, z)), sampler));
        }
    }

    private static CropStandBlockEntity stand(GameTestHelper helper, BlockPos pos, ItemStack seed) {
        helper.setBlock(pos.below(), Blocks.FARMLAND);
        helper.setBlock(pos.above(), Blocks.GLOWSTONE);
        helper.setBlock(pos, ModBlocks.CROP_STAND.get());
        var stand = (CropStandBlockEntity) helper.getBlockEntity(pos);
        helper.assertTrue(stand.plant(seed, false), "Test seed must plant");
        return stand;
    }

    private static int age(CropStandBlockEntity stand) {
        return ((CropBlock) stand.getCropState().getBlock()).getAge(stand.getCropState());
    }

    private static ItemStack geneSeed(GameTestHelper helper, ToleranceType active, ToleranceType inactive) {
        var seed = new ItemStack(Items.POTATO);
        GeneCropGenome.identify(seed, helper.getLevel().random);
        helper.assertTrue(GeneCropGenome.isIdentified(seed), "Potato must be an enabled genetic seed");
        var pair = new CompoundTag();
        pair.putString("active", ForestryAlleles.DEFAULT_TEMPERATURE_TOLERANCES.stream().filter(a -> a.value() == active).findFirst().orElseThrow().alleleId().toString());
        pair.putString("inactive", ForestryAlleles.DEFAULT_TEMPERATURE_TOLERANCES.stream().filter(a -> a.value() == inactive).findFirst().orElseThrow().alleleId().toString());
        seed.getOrCreateTag().getCompound(GeneCropGenome.TAG).put(GeneCropGenome.TEMPERATURE, pair);
        return seed;
    }
}

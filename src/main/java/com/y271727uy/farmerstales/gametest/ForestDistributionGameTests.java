package com.y271727uy.farmerstales.gametest;

import com.mojang.datafixers.util.Pair;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class ForestDistributionGameTests {
    private static final int SIZE = 257;
    private static final int STEP = 32;

    @GameTest(template = "crop_stand_empty", timeoutTicks = 1200)
    public static void climateWindowsProduceBroadForestRegionsAcrossSeeds(GameTestHelper helper) throws IOException {
        var access = helper.getLevel().registryAccess();
        var registry = access.registryOrThrow(Registries.BIOME);
        var original = access.registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
                .getOrThrow(MultiNoiseBiomeSourceParameterLists.OVERWORLD).parameters();
        var revised = MultiNoiseBiomeSource.createFromList(original);
        ((ForestBiomes.ClimateAccess) revised).farmerstales$installForests(registry);
        var former = formerStripLayout(original, registry.getHolderOrThrow(ForestBiomes.PALE_GARDEN),
                registry.getHolderOrThrow(ForestBiomes.DAPPLED_FOREST));
        var output = Path.of("../build/forest-distribution-audit").toAbsolutePath().normalize();
        Files.createDirectories(output);
        StringBuilder report = new StringBuilder("seed,biome,old_cells,new_cells,old_largest_square_blocks,new_largest_square_blocks\n");
        int bestPaleSquare = 0, bestDappledSquare = 0;
        for (long seed : new long[]{0L, 2026L, 271727L}) {
            var sampler = RandomState.create(access.registryOrThrow(Registries.NOISE_SETTINGS)
                    .getOrThrow(NoiseGeneratorSettings.OVERWORLD), access.lookupOrThrow(Registries.NOISE), seed).sampler();
            int[][] before = new int[SIZE][SIZE], after = new int[SIZE][SIZE];
            for (int z = 0; z < SIZE; z++) for (int x = 0; x < SIZE; x++) {
                int blockX = (x - SIZE / 2) * STEP, blockZ = (z - SIZE / 2) * STEP;
                var sample = sampler.sample(blockX / 4, 20, blockZ / 4);
                // Compare surface climate, rather than sampling caves underneath mountains.
                var target = new Climate.TargetPoint(sample.temperature(), sample.humidity(), sample.continentalness(),
                        sample.erosion(), 0L, sample.weirdness());
                before[z][x] = color(former.findValue(target));
                after[z][x] = color(revised.getNoiseBiome(target));
                var vanilla = original.findValue(target);
                if (vanilla.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || vanilla.is(net.minecraft.tags.BiomeTags.IS_RIVER)) {
                    helper.assertTrue(revised.getNoiseBiome(target).is(vanilla.unwrapKey().orElseThrow()),
                            "Forest windows must preserve oceans and rivers");
                }
            }
            writeGrid(output.resolve("seed-" + seed + "-before.csv"), before);
            writeGrid(output.resolve("seed-" + seed + "-after.csv"), after);
            for (int biome = 1; biome <= 2; biome++) {
                int oldSquare = square(before, biome), newSquare = square(after, biome);
                report.append(seed).append(',').append(biome == 1 ? "pale_garden" : "dappled_forest")
                        .append(',').append(count(before, biome)).append(',').append(count(after, biome))
                        .append(',').append(oldSquare * STEP).append(',').append(newSquare * STEP).append('\n');
                if (biome == 1) bestPaleSquare = Math.max(bestPaleSquare, newSquare);
                else bestDappledSquare = Math.max(bestDappledSquare, newSquare);
            }
        }
        Files.writeString(output.resolve("metrics.csv"), report);
        helper.assertTrue(bestPaleSquare >= 6, "Pale Garden needs a solid surface region at least 192 blocks wide");
        helper.assertTrue(bestDappledSquare >= 6, "Dappled Forest needs a solid surface region at least 192 blocks wide");
        helper.succeed();
    }

    private static int color(Holder<Biome> biome) {
        if (biome.is(ForestBiomes.PALE_GARDEN)) return 1;
        if (biome.is(ForestBiomes.DAPPLED_FOREST)) return 2;
        if (biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN) || biome.is(net.minecraft.tags.BiomeTags.IS_RIVER)) return 3;
        if (biome.is(Biomes.TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)) return 4;
        if (biome.is(Biomes.DARK_FOREST)) return 5;
        return 0;
    }

    private static void writeGrid(Path path, int[][] grid) throws IOException {
        var text = new StringBuilder();
        for (var row : grid) {
            for (int x = 0; x < row.length; x++) {
                if (x != 0) text.append(',');
                text.append(row[x]);
            }
            text.append('\n');
        }
        Files.writeString(path, text);
    }

    private static int count(int[][] grid, int biome) {
        int count = 0;
        for (var row : grid) for (int value : row) if (value == biome) count++;
        return count;
    }

    /** Largest entirely filled square catches a biome that only exists as narrow ribbons. */
    private static int square(int[][] grid, int biome) {
        int[][] widths = new int[SIZE + 1][SIZE + 1];
        int largest = 0;
        for (int z = 0; z < SIZE; z++) for (int x = 0; x < SIZE; x++) if (grid[z][x] == biome) {
            widths[z + 1][x + 1] = 1 + Math.min(widths[z][x], Math.min(widths[z + 1][x], widths[z][x + 1]));
            largest = Math.max(largest, widths[z + 1][x + 1]);
        }
        return largest;
    }

    /** Reproduce the old algorithm solely to export an honest before/after comparison. */
    private static Climate.ParameterList<Holder<Biome>> formerStripLayout(Climate.ParameterList<Holder<Biome>> original,
            Holder<Biome> pale, Holder<Biome> dappled) {
        var result = new ArrayList<Pair<Climate.ParameterPoint, Holder<Biome>>>();
        for (var entry : original.values()) {
            var p = entry.getFirst(); var biome = entry.getSecond();
            long min = p.weirdness().min(), max = p.weirdness().max();
            Holder<Biome> replacement = biome.is(Biomes.DARK_FOREST) ? pale : biome.is(Biomes.TAIGA) ? dappled : null;
            if (replacement == null || max - min < 4) { result.add(entry); continue; }
            long boundary = min + (max - min) / (biome.is(Biomes.DARK_FOREST) ? 4 : 3);
            result.add(Pair.of(new Climate.ParameterPoint(p.temperature(), p.humidity(), p.continentalness(), p.erosion(),
                    p.depth(), new Climate.Parameter(min, boundary), p.offset()), replacement));
            result.add(Pair.of(new Climate.ParameterPoint(p.temperature(), p.humidity(), p.continentalness(), p.erosion(),
                    p.depth(), new Climate.Parameter(boundary, max), p.offset()), biome));
        }
        return new Climate.ParameterList<>(result);
    }
}

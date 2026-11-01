package com.y271727uy.farmerstales.gametest;

import com.y271727uy.farmerstales.all.ModBlocks;
import com.y271727uy.farmerstales.config.Config;
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity;
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainEvents;
import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.HeavyRainEvents;
import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.RainIntensity;
import com.y271727uy.farmerstales.gameplay.weather_farming.WeatherFarmingEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("farmerstales")
@PrefixGameTestTemplate(false)
public final class HeavyRainGameTests {
    @GameTest(template = "crop_stand_empty", setupTicks = 20)
    public static void rainVariantsRespectGrowthShelterAndStormLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var weather = (net.minecraft.world.level.storage.ServerLevelData) level.getLevelData();
        int clearTime = weather.getClearWeatherTime(), rainTime = weather.getRainTime(), thunderTime = weather.getThunderTime();
        boolean raining = weather.isRaining(), thundering = weather.isThundering();
        float rainLevel = level.getRainLevel(1), thunderLevel = level.getThunderLevel(1);
        long dayTime = level.getDayTime();
        boolean green = GreenRainEvents.INSTANCE.isActive(level);
        // Build above the reused indoor fixture, where skylight already exists.
        int baseY = helper.absolutePos(BlockPos.ZERO).getY();
        int highest = Math.max(level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                helper.absolutePos(new BlockPos(1, 1, 1))).getY(),
                level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        helper.absolutePos(new BlockPos(3, 1, 1))).getY());
        int y = Math.max(6, highest - baseY + 2);
        var pos = new BlockPos(1, y, 1);
        var standPos = new BlockPos(3, y, 1);
        var absolute = helper.absolutePos(pos);
        var standAbsolute = helper.absolutePos(standPos);
        var wheat = (CropBlock) Blocks.WHEAT;
        try {
            level.setDayTime(6000);
            level.setWeatherParameters(0, 12000, true, false);
            level.setRainLevel(1);
            helper.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            helper.setBlock(pos, wheat.getStateForAge(1));
            helper.setBlock(standPos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
            helper.setBlock(standPos, ModBlocks.CROP_STAND.get());
            var stand = (CropStandBlockEntity) helper.getBlockEntity(standPos);
            stand.plant(new ItemStack(Items.WHEAT_SEEDS), false);
            stand.grow(1);
            helper.assertTrue(level.isRainingAt(absolute.above()), "Fixture must be exposed to actual rain: raining="
                    + level.isRaining() + ", sky=" + level.canSeeSky(absolute.above())
                    + ", precipitation=" + level.getBiome(absolute).value().getPrecipitationAt(absolute));
            helper.assertTrue(Config.INSTANCE.getHeavyRainEnabled() && Config.INSTANCE.getWeatherAffectsCropGrowth(), "Test requires weather growth enabled");

            HeavyRainEvents.INSTANCE.force(level, RainIntensity.HEAVY);
            var before = stand.getCropState();
            for (int i = 0; i < 512; i++) {
                var pre = new BlockEvent.CropGrowEvent.Pre(level, absolute, level.getBlockState(absolute));
                WeatherFarmingEvents.onCropGrowPre(pre);
                helper.assertTrue(pre.getResult() == Event.Result.DEFAULT, "Heavy rain must not slow natural growth");
                WeatherFarmingEvents.onCropGrowPost(new BlockEvent.CropGrowEvent.Post(level, absolute, wheat.getStateForAge(0), wheat.getStateForAge(1)));
                WeatherFarmingEvents.onCropGrowPost(new BlockEvent.CropGrowEvent.Post(level, standAbsolute, wheat.getStateForAge(0), level.getBlockState(standAbsolute)));
            }
            helper.assertTrue(level.getBlockState(absolute).getValue(CropBlock.AGE) == 1, "Heavy rain must suppress field growth bonuses");
            helper.assertTrue(stand.getCropState().equals(before), "Heavy rain must suppress stand growth bonuses");

            HeavyRainEvents.INSTANCE.force(level, RainIntensity.TORRENTIAL);
            int denied = 0;
            for (int i = 0; i < 1024; i++) {
                var pre = new BlockEvent.CropGrowEvent.Pre(level, absolute, level.getBlockState(absolute));
                WeatherFarmingEvents.onCropGrowPre(pre);
                if (pre.getResult() == Event.Result.DENY) denied++;
            }
            if (Config.INSTANCE.getTorrentialRainGrowthPenalty() > 0) {
                helper.assertTrue(denied > 0, "Torrential rain must sometimes deny exposed growth attempts");
            }
            helper.setBlock(pos.above(), Blocks.STONE);
            for (int i = 0; i < 128; i++) {
                var pre = new BlockEvent.CropGrowEvent.Pre(level, absolute, level.getBlockState(absolute));
                WeatherFarmingEvents.onCropGrowPre(pre);
                helper.assertTrue(pre.getResult() == Event.Result.DEFAULT, "Roof must protect crops from torrential slowdown");
            }
            ((net.minecraft.world.level.block.BonemealableBlock) ModBlocks.CROP_STAND.get())
                    .performBonemeal(level, level.random, standAbsolute, level.getBlockState(standAbsolute));
            helper.assertTrue(!stand.getCropState().equals(before), "Torrential rain must not block bone meal");

            var saved = level.getDataStorage().get(tag -> null, "farmerstales_rain_intensity").save(new CompoundTag());
            helper.assertTrue(saved.getBoolean("WasRaining") && saved.getInt("Temperate") == RainIntensity.TORRENTIAL.ordinal()
                    && saved.getInt("Tropical") == RainIntensity.TORRENTIAL.ordinal(), "Both climate results must be persisted");
            HeavyRainEvents.onLevelTick(new TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
            helper.assertTrue(HeavyRainEvents.INSTANCE.intensityAt(level, absolute) == RainIntensity.TORRENTIAL, "Same storm must not reroll");
            level.setWeatherParameters(12000, 0, false, false);
            level.setRainLevel(0);
            HeavyRainEvents.onLevelTick(new TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
            helper.assertTrue(HeavyRainEvents.INSTANCE.intensityAt(level, absolute) == RainIntensity.NORMAL, "Rain stop must clear intensity");
            helper.assertFalse(HeavyRainEvents.INSTANCE.force(level, RainIntensity.HEAVY), "Cannot force rain intensity during clear weather");
        } finally {
            level.setDayTime(dayTime);
            level.setWeatherParameters(clearTime, rainTime, raining, thundering);
            weather.setThunderTime(thunderTime);
            level.setRainLevel(rainLevel);
            level.setThunderLevel(thunderLevel);
            HeavyRainEvents.onLevelTick(new TickEvent.LevelTickEvent(net.minecraftforge.fml.LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
            if (green && raining) GreenRainEvents.INSTANCE.force(level);
        }
        helper.succeed();
    }

    @GameTest(template = "crop_stand_empty")
    public static void seasonalWeightingUsesOneRollAndKeepsRainTypesExclusive(GameTestHelper helper) {
        int baseHeavy = 0, baseTorrential = 0, boostedHeavy = 0, boostedTorrential = 0;
        for (int i = 0; i < 10000; i++) {
            double roll = (i + 0.5) / 10000.0;
            var base = RainIntensity.select(roll, 0.10, 0.02, 1);
            var boosted = RainIntensity.select(roll, 0.10, 0.02, 2);
            if (base == RainIntensity.HEAVY) baseHeavy++;
            if (base == RainIntensity.TORRENTIAL) baseTorrential++;
            if (boosted == RainIntensity.HEAVY) boostedHeavy++;
            if (boosted == RainIntensity.TORRENTIAL) boostedTorrential++;
            helper.assertTrue(boosted.ordinal() >= base.ordinal(), "Seasonal boost must never weaken the same storm roll");
        }
        helper.assertTrue(baseHeavy == 1000 && baseTorrential == 200 && boostedHeavy == 2000 && boostedTorrential == 400,
                "Summer/wet-season weighting must increase both disjoint rain probabilities");
        helper.assertTrue(RainIntensity.select(0.999, 1, 1, 10) == RainIntensity.TORRENTIAL, "Extreme configuration must remain bounded");
        helper.succeed();
    }
}

package com.y271727uy.farmerstales.gameplay.fish_group.pool

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource

class FishPoolDefinition(
    val id: ResourceLocation,
    val environment: Environment,
    minFishCount: Int,
    maxFishCount: Int,
    val fishKing: ResourceLocation?,
    biomes: List<ResourceLocation>,
    weatherRequirement: WeatherRequirement?,
    timeRequirement: TimeRequirement?,
    outputs: List<FishPoolLootEntryDefinition>,
) {
    val minFishCount = minFishCount.coerceAtLeast(1)
    val maxFishCount = maxFishCount.coerceAtLeast(this.minFishCount)
    val biomes = biomes.toList()
    val weatherRequirement = weatherRequirement ?: WeatherRequirement.ANY
    val timeRequirement = timeRequirement ?: TimeRequirement.ANY
    val outputs = outputs.toList()

    init {
        require(this.outputs.isNotEmpty()) { "Fish pool definition must contain at least one output: $id" }
    }

    fun matchesCurrentConditions(level: ServerLevel): Boolean =
        weatherRequirement.matches(level) && timeRequirement.matches(level)

    fun rollFishCount(random: RandomSource): Int =
        if (minFishCount == maxFishCount) maxFishCount else Mth.nextInt(random, minFishCount, maxFishCount)

    fun matchesSpawn(level: ServerLevel, pos: BlockPos): Boolean =
        matchesCurrentConditions(level) && matchesBiome(level, pos)

    fun matchesFishing(level: ServerLevel): Boolean = matchesCurrentConditions(level)

    private fun matchesBiome(level: ServerLevel, pos: BlockPos): Boolean =
        biomes.isEmpty() || level.getBiome(pos).unwrapKey().map { it.location() in biomes }.orElse(false)

    enum class Environment {
        OCEAN,
        RIVER,
    }

    enum class WeatherRequirement {
        ANY,
        CLEAR,
        RAIN,
        THUNDER;

        fun matches(level: ServerLevel): Boolean = when (this) {
            ANY -> true
            CLEAR -> !level.isRaining && !level.isThundering
            RAIN -> level.isRaining && !level.isThundering
            THUNDER -> level.isThundering
        }
    }

    enum class TimeRequirement(val serializedName: String) {
        ANY("null"),
        DAY("day"),
        NIGHT("night");

        fun matches(level: ServerLevel): Boolean = when (this) {
            ANY -> true
            DAY -> level.isDay
            NIGHT -> level.isNight
        }
    }
}

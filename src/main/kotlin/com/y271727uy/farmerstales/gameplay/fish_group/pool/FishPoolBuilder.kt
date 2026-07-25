package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation

class FishPoolBuilder private constructor(
    private val id: ResourceLocation,
    private val environment: FishPoolDefinition.Environment,
) {
    private var minFishCount: Int? = null
    private var maxFishCount = 5
    private var fishKing: ResourceLocation? = null
    private val biomes = mutableListOf<ResourceLocation>()
    private var weatherRequirement: FishPoolDefinition.WeatherRequirement? = null
    private var timeRequirement: FishPoolDefinition.TimeRequirement? = null
    private val outputs = mutableListOf<FishPoolLootEntryDefinition>()

    fun minFishCount(minFishCount: Int) = apply { this.minFishCount = minFishCount }

    fun maxFishCount(maxFishCount: Int) = apply { this.maxFishCount = maxFishCount }

    fun fishKing(fishKing: String?) = apply { this.fishKing = parseNullable(fishKing, "fishKing") }

    fun fishKing(fishKing: ResourceLocation?) = apply { this.fishKing = fishKing }

    fun biome(biome: String?) = biomes(*if (biome == null) emptyArray() else arrayOf(biome))

    fun biome(biome: ResourceLocation?) = biomes(*if (biome == null) emptyArray() else arrayOf(biome))

    fun biomes(vararg biomes: String) = apply {
        this.biomes.clear()
        biomes.mapNotNullTo(this.biomes) { parseNullable(it, "biome") }
    }

    fun biomes(vararg biomes: ResourceLocation) = apply {
        this.biomes.clear()
        this.biomes.addAll(biomes)
    }

    fun weather(weatherRequirement: FishPoolDefinition.WeatherRequirement?) =
        apply { this.weatherRequirement = weatherRequirement }

    fun time(timeRequirement: FishPoolDefinition.TimeRequirement?) = apply { this.timeRequirement = timeRequirement }

    fun output(output: FishPoolLootEntryDefinition) = apply { outputs += output }

    fun outputs(vararg outputs: FishPoolLootEntryDefinition) = apply { this.outputs += outputs }

    fun register(): FishPoolDefinition = FishPoolFactory.register(
        id,
        environment,
        minFishCount ?: maxFishCount,
        maxFishCount,
        fishKing,
        biomes.toList(),
        weatherRequirement,
        timeRequirement,
        *outputs.toTypedArray(),
    )

    companion object {
        @JvmStatic
        fun river(path: String) =
            of(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, path), FishPoolDefinition.Environment.RIVER)

        @JvmStatic
        fun ocean(path: String) =
            of(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, path), FishPoolDefinition.Environment.OCEAN)

        @JvmStatic
        fun of(path: String, environment: FishPoolDefinition.Environment) =
            of(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, path), environment)

        @JvmStatic
        fun of(id: ResourceLocation, environment: FishPoolDefinition.Environment) = FishPoolBuilder(id, environment)

        private fun parseNullable(rawId: String?, fieldName: String): ResourceLocation? {
            if (rawId.isNullOrBlank()) return null
            return requireNotNull(ResourceLocation.tryParse(rawId)) { "Invalid fish pool $fieldName id: $rawId" }
        }
    }
}

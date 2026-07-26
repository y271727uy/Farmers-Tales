package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import java.util.Optional

object FishPoolFactory {
    private val definitions = linkedMapOf<ResourceLocation, FishPoolDefinition>()

    val OCEAN_FISH_POOL: FishPoolDefinition
        get() = FishPoolDefinitions.OCEAN_FISH_POOL
    val RIVER_FISH_POOL: FishPoolDefinition
        get() = FishPoolDefinitions.RIVER_FISH_POOL

    @JvmStatic
    fun river(path: String): FishPoolBuilder = FishPoolBuilder.river(path)

    @JvmStatic
    fun ocean(path: String): FishPoolBuilder = FishPoolBuilder.ocean(path)

    @JvmStatic
    fun builder(path: String, environment: FishPoolDefinition.Environment): FishPoolBuilder =
        FishPoolBuilder.of(path, environment)

    internal fun register(
        id: ResourceLocation,
        environment: FishPoolDefinition.Environment,
        minFishCount: Int,
        maxFishCount: Int,
        fishKing: ResourceLocation?,
        biomes: List<ResourceLocation>,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        spawnSeasons: Set<SeasonWindow>,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition {
        val definition = FishPoolDefinition(
            id,
            environment,
            minFishCount,
            maxFishCount,
            fishKing,
            biomes,
            weatherRequirement,
            timeRequirement,
            spawnSeasons,
            outputs.toList(),
        )
        check(definitions.putIfAbsent(id, definition) == null) { "Duplicate fish pool definition: $id" }
        return definition
    }

    @JvmStatic
    fun get(id: ResourceLocation): Optional<FishPoolDefinition> {
        ensureBuiltIns()
        return Optional.ofNullable(definitions[id])
    }

    @JvmStatic
    fun getAll(): List<FishPoolDefinition> {
        ensureBuiltIns()
        return definitions.values.toList()
    }

    @JvmStatic
    fun getOrDefault(id: ResourceLocation, environment: FishPoolDefinition.Environment): FishPoolDefinition {
        ensureBuiltIns()
        return FishPoolLootManager.resolveDefinition(definitions[id] ?: getDefault(environment))
    }

    @JvmStatic
    fun getDefault(environment: FishPoolDefinition.Environment): FishPoolDefinition {
        ensureBuiltIns()
        return definitions.values.firstOrNull { it.environment == environment }
            ?: error("No fish pool definition registered for environment: $environment")
    }

    @JvmStatic
    fun getAvailable(
        level: ServerLevel,
        pos: BlockPos,
        environment: FishPoolDefinition.Environment,
    ): List<FishPoolDefinition> {
        ensureBuiltIns()
        return definitions.values
            .asSequence()
            .filter { it.environment == environment }
            .map(FishPoolLootManager::resolveDefinition)
            .filter { it.matchesSpawn(level, pos) }
            .toList()
    }

    private fun ensureBuiltIns() {
        FishPoolDefinitions
    }
}

package com.y271727uy.farmerstales.gameplay.fish_group.pool

import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import java.util.Optional

object FishPoolFactory {
    private val definitions = linkedMapOf<ResourceLocation, FishPoolDefinition>()

    @JvmField
    val OCEAN_FISH_POOL = ocean("ocean_fish_pool")
        .minFishCount(1)
        .maxFishCount(8)
        .biomes("minecraft:ocean")
        .output(FishPoolLootEntryDefinition.tag("list:fish_pool/fish_ocean", 100, 1, 5))
        .register()

    @JvmField
    val RIVER_FISH_POOL = river("river_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .biomes("minecraft:river")
        .output(FishPoolLootEntryDefinition.tag("list:fish_pool/fish_river", 100, 1, 3))
        .register()

    @JvmField
    val SALMON_FISH_POOL = riverPool("salmon_fish_pool", 6, "minecraft:salmon")

    @JvmField
    val PUFFERFISH_FISH_POOL = oceanPool("pufferfish_fish_pool", 6, "minecraft:pufferfish")

    @JvmField
    val COD_FISH_POOL = oceanPool("cod_fish_pool", 6, "minecraft:cod")

    @JvmField
    val BLUEGILL_FISH_POOL = riverPool("bluegill_fish_pool", 6, "aquaculture:bluegill")

    @JvmField
    val RAINBOW_TROUT_FISH_POOL = riverPool(
        "rainbow_trout_fish_pool",
        6,
        "aquaculture:rainbow_trout" to 50,
        "aquaculture:brown_trout" to 50,
    )

    @JvmField
    val CARP_FISH_POOL = riverPool("carp_fish_pool", 6, "aquaculture:carp" to 50)

    @JvmField
    val LOBSTER_FISH_POOL = oceanPool("lobster_fish_pool", 5, "crabbersdelight:clawster")

    @JvmField
    val SQUID_FISH_POOL = oceanPool(
        "squid_fish_pool",
        8,
        "culturaldelights:squid",
        "culturaldelights:glow_squid",
    )

    @JvmField
    val CATFISH_FISH_POOL = riverPool("catfish_fish_pool", 8, "aquaculture:catfish")

    @JvmField
    val STRIPED_BASS_FISH_POOL = riverPool(
        "striped_bass_fish_pool",
        5,
        "list:striped_bass",
        "aquaculture:perch",
    )

    @JvmField
    val REDSTONE_BASS_FISH_POOL = oceanPool(
        "redstone_bass_fish_pool",
        6,
        "list:red_grouper",
        "aquaculture:red_grouper",
    )

    @JvmField
    val RIVER_SHRIMP_FISH_POOL = riverPool("river_shrimp_fish_pool", 4, "list:river_shrimp")

    @JvmField
    val RIVER_CRAB_FISH_POOL = riverPool("river_crab_fish_pool", 4, "list:river_crab")

    @JvmField
    val SWIMMING_CRAB_FISH_POOL = oceanPool("swimming_crab_fish_pool", 4, "list:swimming_crab")

    @JvmField
    val LARGE_YELLOW_CROAKER_FISH_POOL = oceanPool(
        "large_yellow_croaker_fish_pool",
        4,
        "list:large_yellow_croaker",
    )

    @JvmField
    val PARROTFISH_FISH_POOL = oceanPool("parrotfish_fish_pool", 4, "list:parrotfish")

    @JvmField
    val SILVER_POMFRET_FISH_POOL = oceanPool("silver_pomfret_fish_pool", 4, "list:silver_pomfret")

    @JvmField
    val CAMOUFLAGE_GROUPER_FISH_POOL = oceanPool(
        "camouflage_grouper_fish_pool",
        6,
        "list:camouflage_grouper",
    )

    @JvmField
    val TURBOT_FISH_POOL = oceanPool("turbot_fish_pool", 4, "list:turbot")

    @JvmField
    val FLUKE_FISH_POOL = oceanPool("fluke_fish_pool", 4, "list:fluke")

    @JvmField
    val SINIPERCA_CHUATSI_FISH_POOL = riverPool("siniperca_chuatsi_fish_pool", 4, "list:siniperca_chuatsi")

    @JvmField
    val SPANISH_MACKEREL_FISH_POOL = oceanPool("spanish_mackerel_fish_pool", 4, "list:spanish_mackerel")

    @JvmField
    val GRASS_CARP_FISH_POOL = riverPool("grass_carp_fish_pool", 4, "list:grass_carp")

    @JvmField
    val CHECKERBOARD_WRASSE_FISH_POOL = oceanPool(
        "checkerboard_wrasse_fish_pool",
        4,
        "list:checkerboard_wrasse",
    )

    @JvmField
    val THREADFIN_BREAM_FISH_POOL = oceanPool("threadfin_bream_fish_pool", 4, "list:threadfin_bream")

    private fun riverPool(path: String, maxFishCount: Int, vararg itemIds: String): FishPoolDefinition =
        riverPool(path, maxFishCount, *itemIds.map { it to 100 }.toTypedArray())

    private fun riverPool(path: String, maxFishCount: Int, vararg outputs: Pair<String, Int>): FishPoolDefinition =
        configuredPool(path, FishPoolDefinition.Environment.RIVER, maxFishCount, "minecraft:river", outputs)

    private fun oceanPool(path: String, maxFishCount: Int, vararg itemIds: String): FishPoolDefinition =
        oceanPool(path, maxFishCount, *itemIds.map { it to 100 }.toTypedArray())

    private fun oceanPool(path: String, maxFishCount: Int, vararg outputs: Pair<String, Int>): FishPoolDefinition =
        configuredPool(path, FishPoolDefinition.Environment.OCEAN, maxFishCount, "minecraft:ocean", outputs)

    private fun configuredPool(
        path: String,
        environment: FishPoolDefinition.Environment,
        maxFishCount: Int,
        biome: String,
        outputs: Array<out Pair<String, Int>>,
    ): FishPoolDefinition {
        val builder = builder(path, environment)
            .minFishCount(1)
            .maxFishCount(maxFishCount)
            .biomes(biome)
        outputs.forEach { (itemId, weight) -> builder.output(FishPoolLootEntryDefinition.item(itemId, weight)) }
        return builder.register()
    }

    @JvmStatic
    fun river(path: String): FishPoolBuilder = FishPoolBuilder.river(path)

    @JvmStatic
    fun ocean(path: String): FishPoolBuilder = FishPoolBuilder.ocean(path)

    @JvmStatic
    fun builder(path: String, environment: FishPoolDefinition.Environment): FishPoolBuilder =
        FishPoolBuilder.of(path, environment)

    @JvmStatic
    fun river(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = river(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun river(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = river(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun river(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = river(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun river(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = river(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun ocean(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = ocean(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun ocean(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = ocean(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun ocean(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = ocean(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun ocean(
        path: String,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = ocean(path)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun register(
        path: String,
        environment: FishPoolDefinition.Environment,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = builder(path, environment)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun register(
        path: String,
        environment: FishPoolDefinition.Environment,
        maxFishCount: Int,
        fishKing: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = builder(path, environment)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun register(
        path: String,
        environment: FishPoolDefinition.Environment,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = builder(path, environment)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun register(
        path: String,
        environment: FishPoolDefinition.Environment,
        maxFishCount: Int,
        fishKing: String?,
        biome: String?,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
        vararg outputs: FishPoolLootEntryDefinition,
    ): FishPoolDefinition = builder(path, environment)
        .maxFishCount(maxFishCount)
        .fishKing(fishKing)
        .biome(biome)
        .weather(weatherRequirement)
        .time(timeRequirement)
        .outputs(*outputs)
        .register()

    @JvmStatic
    fun register(
        id: ResourceLocation,
        environment: FishPoolDefinition.Environment,
        minFishCount: Int,
        maxFishCount: Int,
        fishKing: ResourceLocation?,
        biomes: List<ResourceLocation>,
        weatherRequirement: FishPoolDefinition.WeatherRequirement?,
        timeRequirement: FishPoolDefinition.TimeRequirement?,
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
            outputs.toList(),
        )
        check(definitions.putIfAbsent(id, definition) == null) { "Duplicate fish pool definition: $id" }
        return definition
    }

    @JvmStatic
    fun get(id: ResourceLocation): Optional<FishPoolDefinition> = Optional.ofNullable(definitions[id])

    @JvmStatic
    fun getAll(): List<FishPoolDefinition> = definitions.values.toList()

    @JvmStatic
    fun getOrDefault(id: ResourceLocation, environment: FishPoolDefinition.Environment): FishPoolDefinition =
        FishPoolLootManager.resolveDefinition(definitions[id] ?: getDefault(environment))

    @JvmStatic
    fun getDefault(environment: FishPoolDefinition.Environment): FishPoolDefinition =
        definitions.values.firstOrNull { it.environment == environment }
            ?: error("No fish pool definition registered for environment: $environment")

    @JvmStatic
    fun getAvailable(
        level: ServerLevel,
        pos: BlockPos,
        environment: FishPoolDefinition.Environment,
    ): List<FishPoolDefinition> = definitions.values
        .asSequence()
        .filter { it.environment == environment }
        .map(FishPoolLootManager::resolveDefinition)
        .filter { it.matchesSpawn(level, pos) }
        .toList()
}

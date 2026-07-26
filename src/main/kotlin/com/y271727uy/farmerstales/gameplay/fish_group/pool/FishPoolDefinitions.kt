package com.y271727uy.farmerstales.gameplay.fish_group.pool

import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow

object FishPoolDefinitions {
    internal val RIVER_BIOMES = arrayOf("minecraft:river")
    internal val OCEAN_BIOMES = arrayOf(
        "minecraft:ocean",
        "minecraft:deep_ocean",
        "minecraft:cold_ocean",
        "minecraft:deep_cold_ocean",
        "minecraft:lukewarm_ocean",
        "minecraft:deep_lukewarm_ocean",
        "minecraft:warm_ocean",
        "minecraft:frozen_ocean",
        "minecraft:deep_frozen_ocean",
    )

    @JvmField
    val OCEAN_FISH_POOL = FishPoolFactory.ocean("ocean_fish_pool")
        .minFishCount(1)
        .maxFishCount(8)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.tag("list:fish_pool/fish_ocean", 100, 1, 5))
        .register()

    @JvmField
    val RIVER_FISH_POOL = FishPoolFactory.river("river_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.tag("list:fish_pool/fish_river", 100, 1, 3))
        .register()

    @JvmField
    val SALMON_FISH_POOL = FishPoolFactory.river("salmon_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .spawnSeasons(SeasonWindow.SPRING, SeasonWindow.SUMMER)
        .output(FishPoolLootEntryDefinition.item("minecraft:salmon", 100, 1, 1))
        .register()

    @JvmField
    val PUFFERFISH_FISH_POOL = FishPoolFactory.ocean("pufferfish_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("minecraft:pufferfish", 100, 1, 1))
        .register()

    @JvmField
    val COD_FISH_POOL = FishPoolFactory.ocean("cod_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("minecraft:cod", 100, 1, 1))
        .register()

    @JvmField
    val BLUEGILL_FISH_POOL = FishPoolFactory.river("bluegill_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("aquaculture:bluegill", 100, 1, 1))
        .register()

    @JvmField
    val RAINBOW_TROUT_FISH_POOL = FishPoolFactory.river("rainbow_trout_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("aquaculture:rainbow_trout", 50, 1, 1))
        .output(FishPoolLootEntryDefinition.item("aquaculture:brown_trout", 50, 1, 1))
        .register()

    @JvmField
    val CARP_FISH_POOL = FishPoolFactory.river("carp_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("aquaculture:carp", 50, 1, 1))
        .register()

    @JvmField
    val LOBSTER_FISH_POOL = FishPoolFactory.ocean("lobster_fish_pool")
        .minFishCount(1)
        .maxFishCount(5)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("crabbersdelight:clawster", 100, 1, 1))
        .register()

    @JvmField
    val SQUID_FISH_POOL = FishPoolFactory.ocean("squid_fish_pool")
        .minFishCount(1)
        .maxFishCount(8)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("culturaldelights:squid", 100, 1, 1))
        .output(FishPoolLootEntryDefinition.item("culturaldelights:glow_squid", 100, 1, 1))
        .register()

    @JvmField
    val CATFISH_FISH_POOL = FishPoolFactory.river("catfish_fish_pool")
        .minFishCount(1)
        .maxFishCount(8)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("aquaculture:catfish", 100, 1, 1))
        .register()

    @JvmField
    val STRIPED_BASS_FISH_POOL = FishPoolFactory.river("striped_bass_fish_pool")
        .minFishCount(1)
        .maxFishCount(5)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:striped_bass", 100, 1, 1))
        .output(FishPoolLootEntryDefinition.item("aquaculture:perch", 100, 1, 1))
        .register()

    @JvmField
    val REDSTONE_BASS_FISH_POOL = FishPoolFactory.ocean("redstone_bass_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:red_grouper", 100, 1, 1))
        .output(FishPoolLootEntryDefinition.item("aquaculture:red_grouper", 100, 1, 1))
        .register()

    @JvmField
    val RIVER_SHRIMP_FISH_POOL = FishPoolFactory.river("river_shrimp_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:river_shrimp", 100, 1, 1))
        .register()

    @JvmField
    val RIVER_CRAB_FISH_POOL = FishPoolFactory.river("river_crab_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:river_crab", 100, 1, 1))
        .register()

    @JvmField
    val SWIMMING_CRAB_FISH_POOL = FishPoolFactory.ocean("swimming_crab_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:swimming_crab", 100, 1, 1))
        .register()

    @JvmField
    val LARGE_YELLOW_CROAKER_FISH_POOL = FishPoolFactory.ocean("large_yellow_croaker_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:large_yellow_croaker", 100, 1, 1))
        .register()

    @JvmField
    val PARROTFISH_FISH_POOL = FishPoolFactory.ocean("parrotfish_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:parrotfish", 100, 1, 1))
        .register()

    @JvmField
    val SILVER_POMFRET_FISH_POOL = FishPoolFactory.ocean("silver_pomfret_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:silver_pomfret", 100, 1, 1))
        .register()

    @JvmField
    val CAMOUFLAGE_GROUPER_FISH_POOL = FishPoolFactory.ocean("camouflage_grouper_fish_pool")
        .minFishCount(1)
        .maxFishCount(6)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:camouflage_grouper", 100, 1, 1))
        .register()

    @JvmField
    val TURBOT_FISH_POOL = FishPoolFactory.ocean("turbot_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:turbot", 100, 1, 1))
        .register()

    @JvmField
    val FLUKE_FISH_POOL = FishPoolFactory.ocean("fluke_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:fluke", 100, 1, 1))
        .register()

    @JvmField
    val SINIPERCA_CHUATSI_FISH_POOL = FishPoolFactory.river("siniperca_chuatsi_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:siniperca_chuatsi", 100, 1, 1))
        .register()

    @JvmField
    val SPANISH_MACKEREL_FISH_POOL = FishPoolFactory.ocean("spanish_mackerel_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:spanish_mackerel", 100, 1, 1))
        .register()

    @JvmField
    val GRASS_CARP_FISH_POOL = FishPoolFactory.river("grass_carp_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*RIVER_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:grass_carp", 100, 1, 1))
        .register()

    @JvmField
    val CHECKERBOARD_WRASSE_FISH_POOL = FishPoolFactory.ocean("checkerboard_wrasse_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:checkerboard_wrasse", 100, 1, 1))
        .register()

    @JvmField
    val THREADFIN_BREAM_FISH_POOL = FishPoolFactory.ocean("threadfin_bream_fish_pool")
        .minFishCount(1)
        .maxFishCount(4)
        .fishKing(null)
        .weather(null)
        .time(null)
        .biomes(*OCEAN_BIOMES)
        .output(FishPoolLootEntryDefinition.item("list:threadfin_bream", 100, 1, 1))
        .register()
}

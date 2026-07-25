package com.y271727uy.farmerstales.data

import com.tterrag.registrate.providers.RegistrateLangProvider
import com.y271727uy.farmerstales.FTMod

object ModLangs {
    fun init(provider: RegistrateLangProvider) {
        provider.add("itemGroup.${FTMod.MODID}", "Farmer's Tales")
        provider.add("tooltip.${FTMod.MODID}.tree_seed.cultivable", "Can be cultivated on a tree compost!")
        provider.add("config.jade.plugin_${FTMod.MODID}.tree_stump_jade", "Farmer's Tales: Tree Stump")
        provider.add("config.jade.plugin_${FTMod.MODID}.fish_pool", "Farmer's Tales: Fish Pool")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.title", "Tree Stump Info")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.fertility", "Fertility: %s/50")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.water", "Water: %s/25")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.branches", "Branches: %s/25")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.maintenance_score", "Maintenance: %s/100")
        provider.add("tooltip.${FTMod.MODID}.jade.tree_stump.maintenance_active", "Tree maintenance is acceptable")
        provider.add(
            "tooltip.${FTMod.MODID}.jade.tree_stump.maintenance_paused",
            "Tree maintenance is too low; growth is paused"
        )
        provider.add(
            "tooltip.${FTMod.MODID}.jade.tree_stump.maintenance_bonus",
            "Tree maintenance is excellent, growth speed +%s"
        )
        provider.add("config.jade.plugin_${FTMod.MODID}.animal_breeding_seasons", "Animal breeding seasons")
        provider.add(
            "config.jade.plugin_${FTMod.MODID}.animal_breeding_seasons.desc",
            "Show the breeding seasons for the targeted animal in Jade."
        )
        provider.add(
            "message.${FTMod.MODID}.breeding_blocked",
            "This animal cannot breed in the current season. Breeding seasons: %s"
        )
        provider.add("tooltip.${FTMod.MODID}.breeding_seasons", "Breeding seasons: %s")
        provider.add("season.${FTMod.MODID}.spring", "Spring")
        provider.add("season.${FTMod.MODID}.summer", "Summer")
        provider.add("season.${FTMod.MODID}.autumn", "Autumn")
        provider.add("season.${FTMod.MODID}.winter", "Winter")
        provider.add("season.${FTMod.MODID}.year_round", "Year-round")
        provider.add("season.${FTMod.MODID}.any", "Any")
        provider.add("tooltip.${FTMod.MODID}.fishing_seasons", "Catch seasons: %s")

        provider.add("item.${FTMod.MODID}.bamboo_fishing_rod", "Bamboo Fishing Rod")
        linkedMapOf(
            "ocean_fish_pool" to "Mixed-Species Fish Pool (Ocean)",
            "river_fish_pool" to "Mixed-Species Fish Pool (River)",
            "salmon_fish_pool" to "Salmon Fish Pool",
            "pufferfish_fish_pool" to "Pufferfish Fish Pool",
            "cod_fish_pool" to "Cod Fish Pool",
            "bluegill_fish_pool" to "Bluegill Fish Pool",
            "rainbow_trout_fish_pool" to "Rainbow Trout Fish Pool",
            "carp_fish_pool" to "Carp Fish Pool",
            "lobster_fish_pool" to "Lobster Fish Pool",
            "squid_fish_pool" to "Squid Fish Pool",
            "catfish_fish_pool" to "Catfish Fish Pool",
            "striped_bass_fish_pool" to "Striped Bass Fish Pool",
            "redstone_bass_fish_pool" to "Redstone Bass Fish Pool",
            "river_shrimp_fish_pool" to "River Shrimp Fish Pool",
            "river_crab_fish_pool" to "River Crab Fish Pool",
            "swimming_crab_fish_pool" to "Swimming Crab Fish Pool",
            "large_yellow_croaker_fish_pool" to "Large Yellow Croaker Fish Pool",
            "parrotfish_fish_pool" to "Parrotfish Fish Pool",
            "silver_pomfret_fish_pool" to "Silver Pomfret Fish Pool",
            "camouflage_grouper_fish_pool" to "Camouflage Grouper Fish Pool",
            "turbot_fish_pool" to "Turbot Fish Pool",
            "fluke_fish_pool" to "Fluke Fish Pool",
            "siniperca_chuatsi_fish_pool" to "Siniperca Chuatsi Fish Pool",
            "spanish_mackerel_fish_pool" to "Spanish Mackerel Fish Pool",
            "grass_carp_fish_pool" to "Grass Carp Fish Pool",
            "checkerboard_wrasse_fish_pool" to "Checkerboard Wrasse Fish Pool",
            "threadfin_bream_fish_pool" to "Threadfin Bream Fish Pool",
        ).forEach { (path, name) ->
            provider.add("item.${FTMod.MODID}.$path", name)
            provider.add("entity.${FTMod.MODID}.$path", name)
        }

        provider.add("tooltip.${FTMod.MODID}.fish_group.not_obtainable", "Place this on water to spawn a fish pool.")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.fish_king", "Fish King: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.fish_king.none", "None")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.fish_king.available", "Not obtained")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.fish_king.awarded", "Obtained")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.weather", "Weather: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.weather.any", "Any")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.weather.clear", "Clear")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.weather.rain", "Rain")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.weather.thunder", "Thunderstorm")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.time", "Time: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.time.any", "Any")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.time.day", "Day")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.time.night", "Night")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state", "State: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.fishing", "Fishing in Progress (Fish Pool)")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.available", "Fishable")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.unavailable", "Conditions not met")
    }
}

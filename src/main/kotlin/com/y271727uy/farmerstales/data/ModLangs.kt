package com.y271727uy.farmerstales.data

import com.tterrag.registrate.providers.RegistrateLangProvider
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.ModEntities

object ModLangs {
    fun init(provider: RegistrateLangProvider) {
        provider.add("itemGroup.${FTMod.MODID}", "Farmer's Tales")
        provider.add("block.${FTMod.MODID}.animal_dung", "Animal Dung")
        provider.add("tooltip.${FTMod.MODID}.tree_seed.cultivable", "Can be cultivated on a tree compost!")
        provider.add("tooltip.${FTMod.MODID}.food_expiry_remaining", "Fresh for: %s day(s)")
        provider.add("tooltip.${FTMod.MODID}.food_expiry_date_and_left", "Expires on: %s (in %s)")
        provider.add("tooltip.${FTMod.MODID}.calendar_hour_minute_month_day_year", "%s %s %d, %04d")
        provider.add("tooltip.${FTMod.MODID}.month.january", "January")
        provider.add("tooltip.${FTMod.MODID}.month.february", "February")
        provider.add("tooltip.${FTMod.MODID}.month.march", "March")
        provider.add("tooltip.${FTMod.MODID}.month.april", "April")
        provider.add("tooltip.${FTMod.MODID}.month.may", "May")
        provider.add("tooltip.${FTMod.MODID}.month.june", "June")
        provider.add("tooltip.${FTMod.MODID}.month.july", "July")
        provider.add("tooltip.${FTMod.MODID}.month.august", "August")
        provider.add("tooltip.${FTMod.MODID}.month.september", "September")
        provider.add("tooltip.${FTMod.MODID}.month.october", "October")
        provider.add("tooltip.${FTMod.MODID}.month.november", "November")
        provider.add("tooltip.${FTMod.MODID}.month.december", "December")
        provider.add("tooltip.${FTMod.MODID}.time_delta_hours_minutes", "%s:%s")
        provider.add("tooltip.${FTMod.MODID}.time_delta_days", "%s day(s)")
        provider.add("tooltip.${FTMod.MODID}.time_delta_months_days", "%s month(s) and %s day(s)")
        provider.add("tooltip.${FTMod.MODID}.time_delta_years_months_days", "%s year(s), %s month(s) and %s day(s)")
        provider.add("tooltip.${FTMod.MODID}.food_expired", "Spoiled")
        provider.add("message.${FTMod.MODID}.food_expired", "This food has spoiled.")
        provider.add("tooltip.${FTMod.MODID}.food_freshness.fresh", "Fresh")
        provider.add("tooltip.${FTMod.MODID}.food_freshness.aging", "Aging")
        provider.add("tooltip.${FTMod.MODID}.food_freshness.rotten", "Rotten")
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
        provider.add("config.jade.plugin_${FTMod.MODID}.animal_husbandry", "Animal husbandry")
        provider.add("config.jade.plugin_${FTMod.MODID}.shepherd_dog", "Shepherd dog")
        provider.add(
            "config.jade.plugin_${FTMod.MODID}.animal_breeding_seasons.desc",
            "Show the breeding seasons for the targeted animal in Jade."
        )
        provider.add(
            "message.${FTMod.MODID}.breeding_blocked",
            "This animal cannot breed in the current season. Breeding seasons: %s"
        )
        provider.add("message.${FTMod.MODID}.season_started.spring", "Spring has arrived, new life awakens")
        provider.add("message.${FTMod.MODID}.season_started.summer", "Midsummer is here, the greenery flourishes")
        provider.add("message.${FTMod.MODID}.season_started.autumn", "Autumn has arrived, the branches bear fruit")
        provider.add("message.${FTMod.MODID}.season_started.winter", "Winter descends, frost and snow seal the fields")
        provider.add("message.${FTMod.MODID}.season_started.dry", "The dry season has arrived, the sun scorches")
        provider.add("message.${FTMod.MODID}.season_started.wet", "The rainy season descends, the plants surge to life")
        provider.add("message.${FTMod.MODID}.tropical_climate.monsoon", "Rain lashes the land...")
        provider.add("message.${FTMod.MODID}.tropical_climate.drought", "Heat shimmers from the earth...")
        provider.add("message.${FTMod.MODID}.green_rain.started", "The rain turns an unusual green...")
        provider.add("commands.${FTMod.MODID}.green_rain.success", "Green rain has been forced for this storm.")
        provider.add("commands.${FTMod.MODID}.green_rain.not_raining", "It is not raining in this dimension.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.success", "A wolf raid has been forced.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.no_flock", "There are not enough domesticated sheep nearby.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.failed", "Could not find a spawn position for this flock.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.player_required", "This command must be run by a player.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.break_fence.success", "Raid wolves can now break one fence.")
        provider.add("commands.${FTMod.MODID}.wolf_raid.break_fence.no_raiders", "There are no raid wolves nearby.")
        provider.add("subtitles.${FTMod.MODID}.ambient.rooster.crow", "Rooster crows")
        provider.add("subtitles.${FTMod.MODID}.ambient.wolf.howl", "Wolf howls")
        provider.add("enchantment.${FTMod.MODID}.farmland_protection", "Farmland Protection")
        provider.add("effect.${FTMod.MODID}.fertile_moon", "Fertile Moon")
        provider.add("message.${FTMod.MODID}.fertile_moon.started", "The Fertile Moon has risen.")
        provider.add("commands.${FTMod.MODID}.special_event.fertile_moon.success", "The Fertile Moon has been forced for tonight.")
        provider.add("effect.${FTMod.MODID}.crimson_moon", "Crimson Moon")
        provider.add("message.${FTMod.MODID}.crimson_moon.started", "The Crimson Moon has risen.")
        provider.add("commands.${FTMod.MODID}.special_event.crimson_moon.success", "The Crimson Moon has been forced for tonight.")
        provider.add("message.${FTMod.MODID}.crimson_moon.sleep_confirm", "Are you sure you want to rest? The wolves outside seem restless.")
        provider.add("message.${FTMod.MODID}.crimson_moon.sleep_spared", "The night passed quietly.")
        provider.add("message.${FTMod.MODID}.crimson_moon.sleep_sheep_lost", "While you slept, %s of your sheep were lost.")
        provider.add("commands.${FTMod.MODID}.special_event.unknown", "Unknown special event: %s")
        provider.add("message.${FTMod.MODID}.rat.command.stay", "Your rat will stay here.")
        provider.add("message.${FTMod.MODID}.rat.command.follow", "Your rat will follow you.")
        provider.add("tooltip.${FTMod.MODID}.breeding_seasons", "Breeding seasons: %s")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.wild", "Wild animal")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.domesticated", "Domesticated animal")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.hunger", "Hunger: %s/%s")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.affection", "Affection: %s/100")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.weight", "Weight: %s/%s")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.milking_cooldown", "Milking cooldown: %ss")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.milking_ready", "Ready to milk")
        provider.add("tooltip.${FTMod.MODID}.animal_husbandry.milking_needs_food", "Needs full hunger to milk")
        provider.add("tooltip.${FTMod.MODID}.shepherd_dog.task", "Task: %s")
        provider.add("tooltip.${FTMod.MODID}.shepherd_dog.task.idle", "Idle")
        provider.add("tooltip.${FTMod.MODID}.shepherd_dog.task.herding", "Shepherding")
        provider.add("tooltip.${FTMod.MODID}.shepherd_dog.task.returning", "Returning to owner")
        provider.add("season.${FTMod.MODID}.spring", "Spring")
        provider.add("season.${FTMod.MODID}.summer", "Summer")
        provider.add("season.${FTMod.MODID}.autumn", "Autumn")
        provider.add("season.${FTMod.MODID}.winter", "Winter")
        provider.add("season.${FTMod.MODID}.year_round", "Year-round")
        provider.add("season.${FTMod.MODID}.any", "Any")
        provider.add("tooltip.${FTMod.MODID}.fishing_seasons", "Catch seasons: %s")

        ModEntities.generateLang(provider)
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
        provider.add("tooltip.${FTMod.MODID}.fish_pool.season", "Season: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.season.none", "None")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state", "State: %s")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.fishing", "Fishing in Progress (Fish Pool)")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.available", "Fishable")
        provider.add("tooltip.${FTMod.MODID}.fish_pool.state.unavailable", "Conditions not met")
        provider.add("advancement.${FTMod.MODID}.root.title", "Farmer's Tales")
        provider.add("advancement.${FTMod.MODID}.root.description", "A life shaped by the seasons.")
        provider.add("advancement.${FTMod.MODID}.first_quality_crop.title", "First Fine Harvest")
        provider.add("advancement.${FTMod.MODID}.first_quality_crop.description", "Harvest crops of every quality.")
        provider.add("advancement.${FTMod.MODID}.domesticate_animal.title", "A New Farmhand")
        provider.add("advancement.${FTMod.MODID}.domesticate_animal.description", "Tame every kind of domesticable farm animal.")
        provider.add("advancement.${FTMod.MODID}.breed_domesticated_animals.title", "A Growing Herd")
        provider.add("advancement.${FTMod.MODID}.breed_domesticated_animals.description", "Breed every kind of domesticated farm animal.")
        provider.add("advancement.${FTMod.MODID}.shepherd_dog.title", "A Good Helper")
        provider.add("advancement.${FTMod.MODID}.shepherd_dog.description", "Have a shepherd dog bring a flock together.")
        provider.add("advancement.${FTMod.MODID}.plant_tree.title", "Orchard Open")
        provider.add("advancement.${FTMod.MODID}.plant_tree.description", "Plant a tree on tree compost.")
        provider.add("advancement.${FTMod.MODID}.seasonal_catch.title", "Fishing With the Seasons")
        provider.add("advancement.${FTMod.MODID}.seasonal_catch.description", "Catch from a fish pool in every season.")
        provider.add("advancement.${FTMod.MODID}.fish_king.title", "Fish King")
        provider.add("advancement.${FTMod.MODID}.fish_king.description", "Catch a fish king from a fish pool.")
        provider.add("advancement.${FTMod.MODID}.lush_branches.title", "Lush Branches")
        provider.add("advancement.${FTMod.MODID}.lush_branches.description", "Bring a fruit tree's maintenance to its peak.")
        provider.add("advancement.${FTMod.MODID}.rain_nourishes.title", "Rain Nourishes")
        provider.add("advancement.${FTMod.MODID}.rain_nourishes.description", "Harvest a mature crop in the rain.")
        provider.add("advancement.${FTMod.MODID}.empty_fish_pool.title", "A Clean Sweep")
        provider.add("advancement.${FTMod.MODID}.empty_fish_pool.description", "Catch the final fish from a fish pool.")
        provider.add("advancement.${FTMod.MODID}.armadillo.title", "An Unexpected Visitor")
        provider.add("advancement.${FTMod.MODID}.armadillo.description", "Discover an armadillo.")
        provider.add("advancement.${FTMod.MODID}.nautilus.title", "A Living Fossil?")
        provider.add("advancement.${FTMod.MODID}.nautilus.description", "Discover a nautilus.")
        provider.add("advancement.${FTMod.MODID}.zombie_nautilus.title", "Living Fossil or Zombie?")
        provider.add("advancement.${FTMod.MODID}.zombie_nautilus.description", "Discover a zombie nautilus.")
    }
}

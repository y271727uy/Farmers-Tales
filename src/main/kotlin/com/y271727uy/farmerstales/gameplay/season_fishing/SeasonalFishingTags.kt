package com.y271727uy.farmerstales.gameplay.season_fishing

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.util.RandomSource
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

object SeasonalFishingTags {
    private const val SERENE_SEASONS = "sereneseasons"
    private const val IN_SEASON_WEIGHT_MULTIPLIER = 1.5
    private const val OUT_OF_SEASON_WEIGHT_MULTIPLIER = 0.5

    private val activeSeasonTags = linkedMapOf(
        SeasonWindow.SPRING to itemTag(FTMod.MODID, "active_seasons/spring"),
        SeasonWindow.SUMMER to itemTag(FTMod.MODID, "active_seasons/summer"),
        SeasonWindow.AUTUMN to itemTag(FTMod.MODID, "active_seasons/autumn"),
        SeasonWindow.WINTER to itemTag(FTMod.MODID, "active_seasons/winter")
    )

    private val catchSeasonTags = linkedMapOf(
        SeasonWindow.SPRING to itemTag(SERENE_SEASONS, "spring_fish"),
        SeasonWindow.SUMMER to itemTag(SERENE_SEASONS, "summer_fish"),
        SeasonWindow.AUTUMN to itemTag(SERENE_SEASONS, "autumn_fish"),
        SeasonWindow.WINTER to itemTag(SERENE_SEASONS, "winter_fish")
    )

    private val vanillaFishWeights = mapOf(
        ResourceLocation.withDefaultNamespace("cod") to 60.0,
        ResourceLocation.withDefaultNamespace("salmon") to 25.0,
        ResourceLocation.withDefaultNamespace("pufferfish") to 13.0,
        ResourceLocation.withDefaultNamespace("tropical_fish") to 2.0
    )

    fun activeSeasons(item: Item): List<SeasonWindow> =
        if (catchSeasons(item).isNotEmpty()) emptyList() else taggedSeasons(item, activeSeasonTags)

    fun catchSeasons(item: Item): List<SeasonWindow> = SpecialFishingRules
        .describe(BuiltInRegistries.ITEM.getKey(item))
        ?.seasons
        ?.toList()
        ?: taggedSeasons(item, catchSeasonTags)

    fun isManagedFish(item: Item): Boolean =
        activeSeasons(item).isNotEmpty() || catchSeasons(item).isNotEmpty()

    fun resolveCatch(level: Level, random: RandomSource): ResolvedFishingCatch {
        val currentSeason = SeasonSupport.currentSeasonWindow(level)
        val candidates = BuiltInRegistries.ITEM.asSequence()
            .mapNotNull { candidateFor(it, currentSeason) }
            .toList()
        val baseWeight = candidates.sumOf(WeightedFish::baseWeight)
        val adjustedWeight = candidates.sumOf(WeightedFish::weight)
        val junkWeight = (baseWeight - adjustedWeight).coerceAtLeast(0.0)
        val totalWeight = adjustedWeight + junkWeight
        if (totalWeight <= 0.0) {
            return ResolvedFishingCatch.JUNK
        }

        var roll = random.nextDouble() * totalWeight
        candidates.forEach { candidate ->
            roll -= candidate.weight
            if (roll < 0.0) {
                return ResolvedFishingCatch.FISH(candidate.item)
            }
        }
        return ResolvedFishingCatch.JUNK
    }

    private fun itemTag(namespace: String, path: String): TagKey<Item> =
        TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path))

    private fun baseWeight(item: Item): Double =
        vanillaFishWeights[BuiltInRegistries.ITEM.getKey(item)] ?: 1.0

    private fun taggedSeasons(item: Item, tags: Map<SeasonWindow, TagKey<Item>>): List<SeasonWindow> = tags
        .filter { (_, tag) -> item.builtInRegistryHolder().`is`(tag) }
        .keys
        .toList()

    private fun candidateFor(item: Item, currentSeason: SeasonWindow?): WeightedFish? {
        val specialRule = SpecialFishingRules.describe(BuiltInRegistries.ITEM.getKey(item))
        val catchSeasons = specialRule?.seasons?.toList() ?: taggedSeasons(item, catchSeasonTags)
        if (catchSeasons.isNotEmpty()) {
            if (currentSeason == null || currentSeason !in catchSeasons) {
                return null
            }
            val weight = specialRule?.baseWeight ?: baseWeight(item)
            return WeightedFish(item, weight, weight * IN_SEASON_WEIGHT_MULTIPLIER)
        }

        val activeSeasons = taggedSeasons(item, activeSeasonTags)
        if (activeSeasons.isEmpty()) {
            return null
        }

        val multiplier = if (currentSeason != null && currentSeason in activeSeasons) {
            IN_SEASON_WEIGHT_MULTIPLIER
        } else {
            OUT_OF_SEASON_WEIGHT_MULTIPLIER
        }
        val weight = specialRule?.baseWeight ?: baseWeight(item)
        return WeightedFish(item, weight, weight * multiplier)
    }

    private data class WeightedFish(val item: Item, val baseWeight: Double, val weight: Double)
}

sealed interface ResolvedFishingCatch {
    data class FISH(val item: Item) : ResolvedFishingCatch
    data object JUNK : ResolvedFishingCatch
}

package com.y271727uy.farmerstales.gameplay.season_fishing

import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.resources.ResourceLocation
import java.util.EnumSet

/**
 * Optional hard-coded rules for special fish whose catch window cannot be expressed by a Serene Seasons tag.
 * A special rule takes priority over sereneseasons:*_fish tags.
 */
object SpecialFishingRules {
    private val rules = linkedMapOf<ResourceLocation, SpecialFishingRule>()

    init {
        // fish("examplemod:special_fish")
        //     .catchSeasons(SeasonWindow.SUMMER, SeasonWindow.AUTUMN)
        //     .baseWeight(10.0)
        //     .register()
    }

    fun fish(itemId: String): RuleBuilder = RuleBuilder(ResourceLocation.tryParse(itemId)
        ?: throw IllegalArgumentException("Invalid item id: $itemId"))

    fun describe(itemId: ResourceLocation): SpecialFishingRule? = rules[itemId]

    class RuleBuilder internal constructor(private val itemId: ResourceLocation) {
        private var seasons: Set<SeasonWindow> = EnumSet.allOf(SeasonWindow::class.java)
        private var baseWeight: Double? = null

        fun catchSeasons(vararg values: SeasonWindow): RuleBuilder = apply {
            seasons = if (values.isEmpty()) {
                EnumSet.allOf(SeasonWindow::class.java)
            } else {
                EnumSet.copyOf(values.toList())
            }
        }

        fun baseWeight(value: Double): RuleBuilder = apply {
            require(value > 0.0) { "Fishing weight must be positive" }
            baseWeight = value
        }

        fun register() {
            rules[itemId] = SpecialFishingRule(seasons.toSet(), baseWeight)
        }
    }
}

data class SpecialFishingRule(
    val seasons: Set<SeasonWindow>,
    val baseWeight: Double?
)

package com.y271727uy.farmerstales.gameplay.weather.precipitation

import net.minecraft.world.level.biome.Biome
import kotlin.math.roundToInt

/** Derived annual climate reference in millimetres, including rain and snow; not measured weather. */
object BiomePrecipitation {
    data class Range(val minimumMm: Int, val maximumMm: Int) {
        fun display(): String = if (minimumMm == maximumMm) "$minimumMm" else "$minimumMm-$maximumMm"
    }

    /** Uses the biome's own climate settings so modded and data-pack biomes work automatically. */
    fun annualRange(biome: Biome): Range {
        if (!biome.hasPrecipitation()) return Range(0, 100)
        val downfall = biome.modifiedClimateSettings.downfall()
        val referenceMm = 2000.0 * (if (downfall.isNaN()) 0.0 else downfall.toDouble().coerceIn(0.0, 1.0))
        if (referenceMm == 0.0) return Range(0, 100)
        return Range((referenceMm * 0.75).roundToInt(), (referenceMm * 1.25).roundToInt())
    }
}

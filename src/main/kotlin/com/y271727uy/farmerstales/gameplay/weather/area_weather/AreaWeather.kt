package com.y271727uy.farmerstales.gameplay.weather.area_weather

import java.util.Locale

/** Daily production conditions; these never directly set rain or snow. */
enum class AreaWeather(val id: String) {
    CLEAR("clear"), FOG("fog"), LIGHT_SNOW("light_snow"), HEAVY_SNOW("heavy_snow"),
    NUCLEAR_RADIATION("nuclear_radiation"), THUNDERSHOWERS("thundershowers"), SMOG("smog"),
    LIGHT_RAIN("light_rain"), HEATWAVE("heatwave"), HAIL("hail"), CLOUDY("cloudy"),
    PARTLY_CLOUDY("partly_cloudy"), FROST("frost"), STRONG_WIND("strong_wind"), BREEZE("breeze");

    val translationKey: String get() = "area_weather.farmerstales.$id"

    fun isEligible(month: Int?): Boolean = when (this) {
        LIGHT_SNOW, HEAVY_SNOW, HAIL -> month != null && month in 9..11
        THUNDERSHOWERS, HEATWAVE -> month != null && month in 3..5
        FROST -> month == 8
        else -> true
    }

    companion object {
        // Build candidate arrays once, rather than allocating them on each daily roll.
        private val candidates = (0..12).map { index ->
            entries.filter { it != NUCLEAR_RADIATION && it.isEligible(index.takeIf { it < 12 }) }.toTypedArray()
        }

        /** Radiation gets an independent 1% roll, unaffected by the seasonal pool size. */
        fun select(month: Int?, radiationRoll: Double, candidateIndex: (Int) -> Int): AreaWeather {
            if (radiationRoll < 0.01) return NUCLEAR_RADIATION
            val pool = candidates[month?.takeIf { it in 0..11 } ?: 12]
            return pool[candidateIndex(pool.size)]
        }

        fun byId(id: String): AreaWeather? = entries.firstOrNull { it.id == id.lowercase(Locale.ROOT) }
    }
}

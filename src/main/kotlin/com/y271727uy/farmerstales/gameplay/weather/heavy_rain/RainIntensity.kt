package com.y271727uy.farmerstales.gameplay.weather.heavy_rain

enum class RainIntensity {
    NORMAL, HEAVY, TORRENTIAL;

    companion object {
        /** A single roll chooses mutually exclusive rain types. */
        @JvmStatic
        fun select(roll: Double, heavyChance: Double, torrentialChance: Double, multiplier: Double): RainIntensity {
            val torrential = (torrentialChance * multiplier).coerceIn(0.0, 1.0)
            val heavy = (heavyChance * multiplier).coerceIn(0.0, 1.0 - torrential)
            return when {
                roll < torrential -> TORRENTIAL
                roll < torrential + heavy -> HEAVY
                else -> NORMAL
            }
        }
    }
}

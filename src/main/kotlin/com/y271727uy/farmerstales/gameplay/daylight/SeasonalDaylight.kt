package com.y271727uy.farmerstales.gameplay.daylight

import com.y271727uy.farmerstales.config.Config
import net.minecraft.world.level.Level
import sereneseasons.api.season.Season
import sereneseasons.api.season.SeasonHelper

/** Display-only summer time; the Minecraft world clock is never changed. */
object SeasonalDaylight {
    @JvmStatic
    fun enabledFor(level: Level): Boolean {
        if (!Config.summerDaylightEnabled || level.dimensionType().hasFixedTime()) {
            return false
        }
        return try {
            SeasonHelper.getSeasonState(level)?.season == Season.SUMMER
        } catch (_: Throwable) {
            false
        }
    }

    @JvmStatic
    fun displayDayTime(level: Level): Long =
        level.dayTime + if (enabledFor(level)) Config.summerDaylightExtensionTicks.toLong() else 0L
}

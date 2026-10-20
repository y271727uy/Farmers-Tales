package com.y271727uy.farmerstales.gameplay.special_event.common

import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainEvents
import com.y271727uy.farmerstales.gameplay.special_event.fertile_moon.FertileMoonEvents
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel

/** Shared growth hook for special events. */
object SpecialEventGrowth {
    fun isBonusGrowthActive(level: ServerLevel, pos: BlockPos): Boolean =
        FertileMoonEvents.isActive(level, pos) || GreenRainEvents.isActive(level)
}

package com.y271727uy.farmerstales.gameplay.common

import net.minecraft.world.level.Level

/** Day arithmetic on `Level.dayTime`, shared so every system agrees on what "today" and "night" mean. */
object DayClock {
    const val TICKS_PER_DAY = 24_000L
    const val NIGHT_START = 13_000L
    const val NIGHT_END = 23_999L

    @JvmStatic
    fun dayOf(dayTime: Long): Long = Math.floorDiv(dayTime, TICKS_PER_DAY)

    @JvmStatic
    fun tickOf(dayTime: Long): Long = Math.floorMod(dayTime, TICKS_PER_DAY)

    @JvmStatic
    fun isNight(tickOfDay: Long): Boolean = tickOfDay in NIGHT_START..NIGHT_END
}

/** Whole days elapsed on this level's day clock. */
val Level.dayIndex: Long get() = DayClock.dayOf(dayTime)

/** Position inside the current day, `0..23999`. */
val Level.tickOfDay: Long get() = DayClock.tickOf(dayTime)

val Level.isNightTime: Boolean get() = DayClock.isNight(tickOfDay)

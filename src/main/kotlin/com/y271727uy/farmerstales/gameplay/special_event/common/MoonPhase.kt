package com.y271727uy.farmerstales.gameplay.special_event.common

import net.minecraft.world.level.Level

/** Vanilla `Level.moonPhase` ids: 0 is full moon, 4 is new moon. */
enum class MoonPhase(val id: Int) {
    FULL_MOON(0),
    WANING_GIBBOUS(1),
    LAST_QUARTER(2),
    WANING_CRESCENT(3),
    NEW_MOON(4),
    WAXING_CRESCENT(5),
    FIRST_QUARTER(6),
    WAXING_GIBBOUS(7);

    companion object {
        fun of(level: Level): MoonPhase = of(level.moonPhase)

        fun of(id: Int): MoonPhase = entries.first { it.id == id }
    }
}

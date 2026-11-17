package com.y271727uy.farmerstales.gameplay.special_event.common

/**
 * Client copy of the current dimension's moon-event winner.
 * Independent of the HUD marker effect so milk cannot cancel the night.
 */
object MoonPhaseClientState {
    @JvmField
    @Volatile
    var winnerId: String = ""
    @JvmField @Volatile
    var forecastId: String = ""
    @JvmField @Volatile
    var forecastDay: Long = Long.MIN_VALUE
}

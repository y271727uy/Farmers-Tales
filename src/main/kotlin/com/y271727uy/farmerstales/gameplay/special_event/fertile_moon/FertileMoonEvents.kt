package com.y271727uy.farmerstales.gameplay.special_event.fertile_moon

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhase
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvent
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvents
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel

/** Autumn full-moon night: extra crop and tree growth. */
object FertileMoonEvents {
    const val ID = "fertile_moon"

    private val spec = MoonPhaseEvent(
        id = ID,
        phase = MoonPhase.FULL_MOON,
        season = SeasonWindow.AUTUMN,
        chance = 0.25,
        effect = { FertileMoonEffects.FERTILE_MOON.get() },
        startedMessage = "message.${FTMod.MODID}.fertile_moon.started",
    )

    fun init() {
        MoonPhaseEvents.register(spec)
    }

    fun isActive(level: ServerLevel, @Suppress("UNUSED_PARAMETER") pos: BlockPos): Boolean = isActive(level)

    fun isActive(level: ServerLevel): Boolean = MoonPhaseEvents.isActive(ID, level)

    fun forceForCurrentNight(level: ServerLevel) {
        MoonPhaseEvents.forceForCurrentNight(ID, level)
    }
}

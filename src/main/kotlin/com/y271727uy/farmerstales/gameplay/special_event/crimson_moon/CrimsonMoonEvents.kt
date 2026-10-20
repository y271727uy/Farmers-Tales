package com.y271727uy.farmerstales.gameplay.special_event.crimson_moon

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhase
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvent
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvents
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel

/** Full-moon night with no season gate. Sleeping is delayed, and the livestock raid does not break at dawn's edge. */
object CrimsonMoonEvents {
    const val ID = "crimson_moon"

    private val spec = MoonPhaseEvent(
        id = ID,
        phase = MoonPhase.FULL_MOON,
        season = null,
        chance = 0.12,
        effect = { CrimsonMoonEffects.CRIMSON_MOON.get() },
        startedMessage = "message.${FTMod.MODID}.crimson_moon.started",
        moonTexture = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/environment/crimson_moon.png"),
        fogColor = 0x420D03,
        haloColor = 0xC41E3A,
    )

    fun init() {
        MoonPhaseEvents.register(spec)
    }

    fun isActive(level: ServerLevel): Boolean = MoonPhaseEvents.isActive(ID, level)

    fun forceForCurrentNight(level: ServerLevel) {
        MoonPhaseEvents.forceForCurrentNight(ID, level)
    }
}

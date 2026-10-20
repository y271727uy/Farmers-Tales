package com.y271727uy.farmerstales.gameplay.special_event.common

import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.effect.MobEffect

/**
 * One night-long moon event. Gameplay stays in the event's own package;
 * this only describes when it may roll and which marker effect to show.
 *
 * [season] is ignored when Serene Seasons is not loaded. `null` means any season.
 * [chance] is a lottery weight, not an independent probability. Eligible events
 * on a night are drawn as a single exclusive roll; leftover weight
 * `max(0, 1 - sum(weights))` is the chance that nothing happens.
 * [moonTexture] is a `moon_phases.png`-layout atlas used only while this event
 * is active. [fogColor] is a packed RGB night fog tint; `null` keeps vanilla.
 * [haloColor] tints an additive celestial quad around the moon; `null` skips it.
 */
class MoonPhaseEvent(
    val id: String,
    val phase: MoonPhase,
    val season: SeasonWindow? = null,
    val chance: Double,
    val effect: () -> MobEffect,
    val startedMessage: String,
    val moonTexture: ResourceLocation? = null,
    val fogColor: Int? = null,
    val haloColor: Int? = null,
)

package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryStatus
import net.minecraft.world.entity.animal.Sheep

/** Tuned conservatively so a large animal pen cannot cause navigation spikes. */
object ShepherdingRules {
    const val SCAN_INTERVAL_TICKS = 5
    // All sheep movement caused by shepherding uses the same deliberately calm pace.
    const val PLAYER_SHEEP_MOVEMENT_SPEED = 0.8
    const val DOG_SHEEP_MOVEMENT_SPEED = 0.8

    fun isEligible(sheep: Sheep): Boolean =
        AnimalHusbandryState.status(sheep) == AnimalHusbandryStatus.DOMESTICATED
}

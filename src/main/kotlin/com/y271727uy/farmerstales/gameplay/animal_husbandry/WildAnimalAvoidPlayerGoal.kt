package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.gameplay.pathfinding.FastPathNavigation
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Animal
import java.util.EnumSet

/** Moves only when the player-centred scheduler has issued an escape request. */
class WildAnimalAvoidPlayerGoal(private val animal: Animal) : Goal() {
    private var request: WildAnimalAvoidance.EscapeRequest? = null

    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean {
        request = WildAnimalAvoidance.activeRequest(animal)
        return request != null
    }

    override fun canContinueToUse(): Boolean {
        if (AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.WILD) {
            return false
        }
        request = WildAnimalAvoidance.activeRequest(animal)
        return request != null
    }

    override fun start() {
        request?.let(::moveToRequest)
    }

    override fun stop() {
        request = null
        FastPathNavigation.stop(animal)
    }

    private fun moveToRequest(request: WildAnimalAvoidance.EscapeRequest) {
        FastPathNavigation.moveTo(animal, request.targetX, request.targetY, request.targetZ, 1.8)
    }
}

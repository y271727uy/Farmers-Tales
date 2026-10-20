package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Sheep
import java.util.EnumSet

/** Keeps returned sheep from wandering again while their dog is still collecting stragglers. */
class SheepDogFlockConstraintGoal(private val sheep: Sheep) : Goal() {
    init {
        flags = EnumSet.of(Flag.MOVE)
    }

    override fun canUse(): Boolean = ShepherdingEvents.shouldHoldForDog(sheep)

    override fun canContinueToUse(): Boolean = ShepherdingEvents.shouldHoldForDog(sheep)

    override fun tick() {
        ShepherdingEvents.holdOrReturnToDogFlock(sheep)
    }
}

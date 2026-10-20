package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Wolf
import java.util.EnumSet
import kotlin.math.hypot

/** Raid wolves back off from goat horns and lit campfires. */
class RaidWolfFleeDeterrentGoal(private val wolf: Wolf) : Goal() {
    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean = RaidWolfDeterrence.shouldFlee(wolf)

    override fun canContinueToUse(): Boolean = RaidWolfDeterrence.shouldFlee(wolf)

    override fun start() {
        wolf.target = null
        wolf.lastHurtByMob = null
        wolf.navigation.stop()
    }

    override fun stop() {
        wolf.navigation.stop()
    }

    override fun tick() {
        val origin = RaidWolfDeterrence.fleeOrigin(wolf) ?: return
        wolf.target = null
        wolf.lookControl.setLookAt(origin.x, origin.y, origin.z)
        val dx = wolf.x - origin.x
        val dz = wolf.z - origin.z
        val length = hypot(dx, dz).coerceAtLeast(0.001)
        wolf.navigation.moveTo(
            wolf.x + dx / length * FLEE_DISTANCE,
            wolf.y,
            wolf.z + dz / length * FLEE_DISTANCE,
            FLEE_SPEED,
        )
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    companion object {
        private const val FLEE_DISTANCE = 10.0
        private const val FLEE_SPEED = 1.3
    }
}

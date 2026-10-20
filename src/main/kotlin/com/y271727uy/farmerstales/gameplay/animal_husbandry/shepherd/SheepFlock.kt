package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.Entity
import kotlin.math.hypot

/** Computes a bounded flock's centroid and each sheep's combined escape/cohesion target. */
object SheepFlock {
    private const val ESCAPE_WEIGHT = 0.85
    private const val COHESION_WEIGHT = 0.65
    private const val TARGET_DISTANCE = 4.0

    fun centerOf(sheep: Collection<Sheep>): Center {
        val count = sheep.size.coerceAtLeast(1)
        return Center(
            sheep.sumOf { it.x } / count,
            sheep.sumOf { it.y } / count,
            sheep.sumOf { it.z } / count,
        )
    }

    fun targetFor(
        sheep: Sheep,
        driver: Entity,
        center: Center,
        maxDistanceFromDriver: Double? = null,
    ): Target {
        val (awayX, awayZ) = normalized(sheep.x - driver.x, sheep.z - driver.z, 1.0, 0.0)
        val (cohesionX, cohesionZ) = normalized(center.x - sheep.x, center.z - sheep.z, 0.0, 0.0)
        val (directionX, directionZ) = normalized(
            awayX * ESCAPE_WEIGHT + cohesionX * COHESION_WEIGHT,
            awayZ * ESCAPE_WEIGHT + cohesionZ * COHESION_WEIGHT,
            awayX,
            awayZ,
        )
        var targetX = sheep.x + directionX * TARGET_DISTANCE
        var targetZ = sheep.z + directionZ * TARGET_DISTANCE
        maxDistanceFromDriver?.let { maximumDistance ->
            val offsetX = targetX - driver.x
            val offsetZ = targetZ - driver.z
            val distance = hypot(offsetX, offsetZ)
            if (distance > maximumDistance) {
                targetX = driver.x + offsetX / distance * maximumDistance
                targetZ = driver.z + offsetZ / distance * maximumDistance
            }
        }
        return Target(targetX, sheep.y, targetZ)
    }

    private fun normalized(x: Double, z: Double, fallbackX: Double, fallbackZ: Double): Pair<Double, Double> {
        val length = hypot(x, z)
        return if (length < 0.001) fallbackX to fallbackZ else x / length to z / length
    }

    data class Center(val x: Double, val y: Double, val z: Double)

    data class Target(val x: Double, val y: Double, val z: Double) {
        fun distanceToSqr(other: Target): Double {
            val deltaX = x - other.x
            val deltaY = y - other.y
            val deltaZ = z - other.z
            return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
        }
    }
}

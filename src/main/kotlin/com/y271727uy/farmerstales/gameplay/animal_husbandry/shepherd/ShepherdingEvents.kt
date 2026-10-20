package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModItems
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct port of Farmers Dislikes' HerdingManager behaviour, with only domestic sheep eligible.
 * There is deliberately no persisted flock, second sheep AI, or competing movement queue.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object ShepherdingEvents {
    private val recentlyHerdedCells = WeakHashMap<ServerLevel, MutableMap<Long, Long>>()
    private val dogFlockConstraints = WeakHashMap<Sheep, DogFlockConstraint>()
    private val dogPushUntil = WeakHashMap<Sheep, Long>()

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (event.server.tickCount % STAFF_UPDATE_INTERVAL_TICKS != 0) return
        event.server.playerList.players.forEach { player ->
            if (!player.isSpectator && isHoldingCrook(player)) tightenFlock(player)
        }
    }

    /** Called by the dog every four ticks after it reaches the outer pressure position. */
    fun driveNearbySheep(dog: Wolf, flock: Collection<Sheep>, center: SheepFlock.Center) {
        if (ShepherdDogState.task(dog) != ShepherdDogTask.HERDING) return
        flock.asSequence()
            .filter(ShepherdingRules::isEligible)
            .filter { it.distanceToSqr(dog) <= DOG_REACTION_RADIUS_SQR }
            .forEach { sheep ->
                dogPushUntil[sheep] = sheep.level().gameTime + DOG_PUSH_CONTROL_TICKS
                steerSheep(sheep, dog, center, ShepherdingRules.DOG_SHEEP_MOVEMENT_SPEED)
            }
    }

    /** Refreshes a task-local containment area. It expires automatically when the dog stops. */
    fun constrainDogFlock(dog: Wolf, flock: Collection<Sheep>, center: SheepFlock.Center) {
        val radius = DOG_HOLD_RADIUS_BASE + sqrt(flock.size.toDouble()) * DOG_HOLD_RADIUS_PER_SHEEP
        val expiresAt = dog.level().gameTime + DOG_CONSTRAINT_REFRESH_TICKS
        flock.asSequence().filter(ShepherdingRules::isEligible).forEach { sheep ->
            dogFlockConstraints[sheep] = DogFlockConstraint(dog, center, radius, expiresAt)
        }
    }

    fun shouldHoldForDog(sheep: Sheep): Boolean {
        val constraint = activeDogConstraint(sheep) ?: return false
        return sheep.level().gameTime >= (dogPushUntil[sheep] ?: Long.MIN_VALUE) && constraint.dog.isAlive
    }

    fun holdOrReturnToDogFlock(sheep: Sheep) {
        val constraint = activeDogConstraint(sheep) ?: return
        val dx = sheep.x - constraint.center.x
        val dz = sheep.z - constraint.center.z
        if (dx * dx + dz * dz <= constraint.radius * constraint.radius) {
            sheep.navigation.stop()
            return
        }
        sheep.navigation.moveTo(
            constraint.center.x,
            sheep.y,
            constraint.center.z,
            ShepherdingRules.DOG_SHEEP_MOVEMENT_SPEED,
        )
    }

    private fun tightenFlock(player: ServerPlayer) {
        val level = player.level() as? ServerLevel ?: return
        if (!claimHerdingCell(level, player.position())) return
        val flock = level.getEntitiesOfClass(Sheep::class.java, player.boundingBox.inflate(STAFF_SEARCH_RANGE, 8.0, STAFF_SEARCH_RANGE)) {
            ShepherdingRules.isEligible(it)
        }
        if (flock.size < MIN_FLOCK_SIZE) return

        val center = SheepFlock.centerOf(flock)
        val clusterRadius = CLUSTER_RADIUS_BASE + sqrt(flock.size.toDouble()) * CLUSTER_RADIUS_PER_SHEEP
        flock.forEach { sheep ->
            if (sheep.distanceToSqr(player) <= STAFF_REACTION_RADIUS_SQR) {
                steerSheep(sheep, player, center, ShepherdingRules.PLAYER_SHEEP_MOVEMENT_SPEED)
                return@forEach
            }

            val dx = sheep.x - center.x
            val dz = sheep.z - center.z
            if (dx * dx + dz * dz <= clusterRadius * clusterRadius) return@forEach
            val angle = Math.floorMod(sheep.id, flock.size) * Math.PI * 2.0 / flock.size
            sheep.navigation.moveTo(
                center.x + cos(angle) * clusterRadius * 0.6,
                center.y,
                center.z + sin(angle) * clusterRadius * 0.6,
                OUTER_SHEEP_SPEED,
            )
        }
    }

    private fun steerSheep(sheep: Sheep, driver: net.minecraft.world.entity.Entity, center: SheepFlock.Center, speed: Double) {
        val target = SheepFlock.targetFor(sheep, driver, center)
        sheep.navigation.moveTo(target.x, target.y, target.z, speed)
    }

    private fun activeDogConstraint(sheep: Sheep): DogFlockConstraint? {
        val constraint = dogFlockConstraints[sheep] ?: return null
        if (sheep.isRemoved || !ShepherdingRules.isEligible(sheep) || sheep.level().gameTime >= constraint.expiresAt ||
            ShepherdDogState.task(constraint.dog) != ShepherdDogTask.HERDING || constraint.dog.isRemoved
        ) {
            dogFlockConstraints.remove(sheep)
            dogPushUntil.remove(sheep)
            return null
        }
        return constraint
    }

    private fun claimHerdingCell(level: ServerLevel, position: Vec3): Boolean {
        val cellX = kotlin.math.floor(position.x / HERDING_CELL_SIZE).toInt()
        val cellZ = kotlin.math.floor(position.z / HERDING_CELL_SIZE).toInt()
        val key = (cellX.toLong() and 0xffffffffL) or ((cellZ.toLong() and 0xffffffffL) shl 32)
        val now = level.gameTime
        val cells = recentlyHerdedCells.getOrPut(level) { HashMap() }
        val previous = cells.put(key, now)
        if (cells.size > MAX_TRACKED_CELLS) cells.entries.removeIf { now - it.value > CELL_EXPIRY_TICKS }
        return previous != now
    }

    private fun isHoldingCrook(player: ServerPlayer): Boolean =
        player.mainHandItem.`is`(ModItems.SHEPHERDS_CROOK.get()) ||
            player.offhandItem.`is`(ModItems.SHEPHERDS_CROOK.get())

    private const val STAFF_UPDATE_INTERVAL_TICKS = 10
    private const val STAFF_SEARCH_RANGE = 16.0
    private const val STAFF_REACTION_RADIUS_SQR = 4.5 * 4.5
    private const val DOG_REACTION_RADIUS_SQR = 4.5 * 4.5
    private const val MIN_FLOCK_SIZE = 2
    private const val CLUSTER_RADIUS_BASE = 1.5
    private const val CLUSTER_RADIUS_PER_SHEEP = 0.35
    private const val OUTER_SHEEP_SPEED = 0.8
    private const val HERDING_CELL_SIZE = 16.0
    private const val CELL_EXPIRY_TICKS = 20L
    private const val MAX_TRACKED_CELLS = 1024
    private const val DOG_HOLD_RADIUS_BASE = 1.8
    private const val DOG_HOLD_RADIUS_PER_SHEEP = 0.7
    private const val DOG_CONSTRAINT_REFRESH_TICKS = 30L
    private const val DOG_PUSH_CONTROL_TICKS = 10L

    private data class DogFlockConstraint(
        val dog: Wolf,
        val center: SheepFlock.Center,
        val radius: Double,
        val expiresAt: Long,
    )
}

package com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModItems
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.EnumSet
import java.util.WeakHashMap
import kotlin.math.hypot
import kotlin.math.sqrt

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object ShepherdDogEvents {
    private val drivingDogs = WeakHashMap<Wolf, Unit>()

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWolfInteract(event: PlayerInteractEvent.EntityInteract) = assignHerdingTask(event, event.target as? Wolf)

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onWolfInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) = assignHerdingTask(event, event.target as? Wolf)

    private fun assignHerdingTask(event: PlayerInteractEvent, wolf: Wolf?) {
        if (!event.itemStack.`is`(ModItems.SHEPHERDS_CROOK.get()) || wolf == null) {
            return
        }
        if (!wolf.isTame || wolf.ownerUUID != event.entity.uuid || event.level.isClientSide) {
            return
        }
        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
        wolf.isOrderedToSit = false
        ShepherdDogState.setTask(wolf, ShepherdDogTask.HERDING)
    }

    fun hasHerdingDog(player: ServerPlayer): Boolean = player.level().getEntitiesOfClass(
        Wolf::class.java,
        player.boundingBox.inflate(MAX_TASK_DISTANCE, 16.0, MAX_TASK_DISTANCE),
    ) { wolf -> wolf.ownerUUID == player.uuid && ShepherdDogState.task(wolf) == ShepherdDogTask.HERDING }
        .isNotEmpty()

    fun setDriving(wolf: Wolf, driving: Boolean) {
        if (driving) drivingDogs[wolf] = Unit else drivingDogs.remove(wolf)
    }

    fun drivingDogNear(sheep: Sheep): Wolf? = sheep.level().getEntitiesOfClass(
        Wolf::class.java,
        sheep.boundingBox.inflate(DOG_REACTION_RANGE, 2.0, DOG_REACTION_RANGE),
    ) { dog -> drivingDogs.containsKey(dog) && ShepherdDogState.task(dog) == ShepherdDogTask.HERDING }
        .minByOrNull { it.distanceToSqr(sheep) }

    private const val DOG_REACTION_RANGE = 4.5
    private const val MAX_TASK_DISTANCE = 48.0
}

/**
 * The dog has two explicit phases, matching the reference mod's stable behaviour:
 * first reach the outside staging point of the most spread-out sheep, then keep walking
 * into a nearer pressure point while only nearby sheep react.
 */
class ShepherdDogGoal(private val wolf: Wolf) : Goal() {
    private var flock: List<Sheep> = emptyList()
    private var refreshCooldown = 0
    private var settledTicks = 0
    private var noFlockTicks = 0
    private var workCenter: SheepFlock.Center? = null

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean = isWorking()

    override fun canContinueToUse(): Boolean = isWorking()

    override fun start() {
        flock = emptyList()
        refreshCooldown = 0
        settledTicks = 0
        noFlockTicks = 0
        workCenter = null
    }

    override fun stop() {
        ShepherdDogEvents.setDriving(wolf, false)
        flock = emptyList()
        wolf.navigation.stop()
    }

    override fun tick() {
        val owner = wolf.owner as? LivingEntity ?: return cancelTask()
        if (!owner.isAlive) return cancelTask()

        when (ShepherdDogState.task(wolf)) {
            ShepherdDogTask.HERDING -> herd(owner)
            ShepherdDogTask.RETURNING -> returnToOwner(owner)
            ShepherdDogTask.IDLE -> Unit
        }
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun herd(owner: LivingEntity) {
        if (horizontalDistanceSqr(wolf.position(), owner.position()) > MAX_OWNER_DISTANCE_SQR) {
            beginReturn()
            return
        }

        if (refreshCooldown-- <= 0) {
            refreshFlock(owner)
            refreshCooldown = FLOCK_REFRESH_TICKS
        }
        if (flock.size < MIN_FLOCK_SIZE) {
            ShepherdDogEvents.setDriving(wolf, false)
            settledTicks = 0
            if (++noFlockTicks >= NO_FLOCK_RETURN_TICKS) beginReturn() else waitNearOwner(owner)
            return
        }
        noFlockTicks = 0

        // A static work anchor prevents distant wanderers from dragging the calculated center
        // away every refresh and making the dog chase an endlessly moving target.
        val center = workCenter ?: SheepFlock.centerOf(flock).also { workCenter = it }
        if (wolf.tickCount % FLOCK_CONSTRAINT_INTERVAL_TICKS == 0) {
            ShepherdingEvents.constrainDogFlock(wolf, flock, center)
        }
        val outer = flock.maxByOrNull { horizontalDistanceSqr(it.position(), Vec3(center.x, center.y, center.z)) } ?: return
        val outward = horizontalUnit(outer.position().subtract(center.x, center.y, center.z))
        val radius = hypot(outer.x - center.x, outer.z - center.z)
        val settledRadius = SETTLED_RADIUS_BASE + SETTLED_RADIUS_PER_SHEEP * sqrt(flock.size.toDouble())
        wolf.lookControl.setLookAt(outer, 30.0f, 30.0f)

        if (radius <= settledRadius) {
            ShepherdDogEvents.setDriving(wolf, false)
            wolf.navigation.stop()
            if (++settledTicks >= SETTLED_CONFIRMATION_TICKS) {
                (wolf.owner as? ServerPlayer)?.let {
                    FarmersTalesAdvancements.award(it, "animal_husbandry/shepherd_dog")
                }
                beginReturn()
            }
            return
        }
        settledTicks = 0
        if (outward == Vec3.ZERO) return

        val staging = outer.position().add(outward.scale(STAGING_DISTANCE))
        if (horizontalDistanceSqr(wolf.position(), staging) > STAGING_REACHED_DISTANCE_SQR) {
            ShepherdDogEvents.setDriving(wolf, false)
            wolf.navigation.moveTo(staging.x, staging.y, staging.z, STAGING_SPEED)
            return
        }

        // Do not stand still once staged. Moving toward this nearer point is the actual pressure.
        ShepherdDogEvents.setDriving(wolf, true)
        val pressure = outer.position().add(outward.scale(PRESSURE_DISTANCE))
        wolf.navigation.moveTo(pressure.x, pressure.y, pressure.z, PRESSURE_SPEED)
        if (wolf.tickCount % SHEEP_PUSH_INTERVAL_TICKS == 0) {
            ShepherdingEvents.driveNearbySheep(wolf, flock, center)
        }
    }

    private fun refreshFlock(owner: LivingEntity) {
        val level = wolf.level() as? net.minecraft.server.level.ServerLevel ?: run {
            flock = emptyList()
            return
        }
        flock = level.getEntitiesOfClass(Sheep::class.java, owner.boundingBox.inflate(FLOCK_SEARCH_RADIUS, 8.0, FLOCK_SEARCH_RADIUS)) {
            ShepherdingRules.isEligible(it)
        }
    }

    private fun returnToOwner(owner: LivingEntity) {
        wolf.lookControl.setLookAt(owner, 30.0f, 30.0f)
        if (wolf.distanceToSqr(owner) <= RETURN_DISTANCE_SQR) {
            wolf.navigation.stop()
            ShepherdDogState.setTask(wolf, ShepherdDogTask.IDLE)
            wolf.isOrderedToSit = true
            return
        }
        wolf.navigation.moveTo(owner, RETURN_SPEED)
    }

    private fun waitNearOwner(owner: LivingEntity) {
        if (wolf.distanceToSqr(owner) > WAIT_DISTANCE_SQR) wolf.navigation.moveTo(owner, RETURN_SPEED)
        else wolf.navigation.stop()
    }

    private fun beginReturn() {
        ShepherdDogEvents.setDriving(wolf, false)
        ShepherdDogState.setTask(wolf, ShepherdDogTask.RETURNING)
    }

    private fun cancelTask() {
        ShepherdDogEvents.setDriving(wolf, false)
        ShepherdDogState.setTask(wolf, ShepherdDogTask.IDLE)
        wolf.navigation.stop()
    }

    private fun isWorking(): Boolean =
        ShepherdDogState.task(wolf) != ShepherdDogTask.IDLE && wolf.isTame && !wolf.isOrderedToSit && wolf.target == null

    private fun horizontalDistanceSqr(first: Vec3, second: Vec3): Double {
        val x = first.x - second.x
        val z = first.z - second.z
        return x * x + z * z
    }

    private fun horizontalUnit(vector: Vec3): Vec3 {
        val length = hypot(vector.x, vector.z)
        return if (length < 0.001) Vec3.ZERO else Vec3(vector.x / length, 0.0, vector.z / length)
    }

    private companion object {
        const val FLOCK_SEARCH_RADIUS = 30.0
        const val FLOCK_REFRESH_TICKS = 40
        const val MIN_FLOCK_SIZE = 2
        const val MAX_OWNER_DISTANCE_SQR = 48.0 * 48.0
        const val NO_FLOCK_RETURN_TICKS = 80
        const val SETTLED_CONFIRMATION_TICKS = 60
        const val SETTLED_RADIUS_BASE = 1.8
        const val SETTLED_RADIUS_PER_SHEEP = 0.7
        const val STAGING_DISTANCE = 3.5
        const val PRESSURE_DISTANCE = 1.1
        const val STAGING_REACHED_DISTANCE_SQR = 2.25
        const val SHEEP_PUSH_INTERVAL_TICKS = 4
        const val FLOCK_CONSTRAINT_INTERVAL_TICKS = 10
        const val STAGING_SPEED = 1.25
        const val PRESSURE_SPEED = 1.05
        const val RETURN_SPEED = 1.25
        const val RETURN_DISTANCE_SQR = 9.0
        const val WAIT_DISTANCE_SQR = 25.0
    }
}

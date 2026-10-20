package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.UUID
import java.util.WeakHashMap

/**
 * Player-centred wild-animal avoidance. The only range query happens once per
 * scan interval for each relevant player; animal goals only consume requests.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object WildAnimalAvoidance {
    private const val AVOIDANCE_RANGE = 8.0
    private const val REQUEST_DURATION_TICKS = 20L
    private const val ESCAPE_DISTANCE = 6.0
    private val installedGoals = WeakHashMap<Animal, Unit>()
    private val requests = WeakHashMap<Animal, EscapeRequest>()

    @JvmStatic
    @SubscribeEvent
    fun onAnimalJoinLevel(event: EntityJoinLevelEvent) {
        val animal = event.entity as? Animal ?: return
        if (event.level.isClientSide || !AnimalHusbandryRules.isSupported(animal)) {
            return
        }

        // Migrate animals that existed before this system, and treat command/
        // spawn-egg animals consistently with naturally generated animals.
        if (AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.UNMANAGED) {
            AnimalHusbandryState.markWild(animal)
        }

        if (AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.WILD ||
            installedGoals.put(animal, Unit) != null
        ) {
            return
        }
        animal.goalSelector.addGoal(1, WildAnimalAvoidPlayerGoal(animal))
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) {
            return
        }
        val player = event.player as? ServerPlayer ?: return
        if (player.tickCount % Config.wildAnimalAvoidanceScanIntervalTicks != 0 || !canTriggerAvoidance(player)) {
            return
        }

        val rangeSquared = AVOIDANCE_RANGE * AVOIDANCE_RANGE
        player.level().getEntitiesOfClass(Animal::class.java, player.boundingBox.inflate(AVOIDANCE_RANGE)) { animal ->
            AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.WILD &&
                AnimalHusbandryRules.isSupported(animal) &&
                animal.distanceToSqr(player) <= rangeSquared
        }.forEach { animal -> requestEscape(animal, player) }
    }

    fun activeRequest(animal: Animal): EscapeRequest? {
        val request = requests[animal] ?: return null
        if (animal.level().gameTime >= request.expiresAt || !canTriggerAvoidance(animal.level().server?.playerList?.getPlayer(request.playerId))) {
            requests.remove(animal)
            return null
        }
        return request
    }

    private fun requestEscape(animal: Animal, player: ServerPlayer) {
        val active = activeRequest(animal)
        if (active != null) {
            return
        }

        var directionX = animal.x - player.x
        var directionZ = animal.z - player.z
        val length = kotlin.math.hypot(directionX, directionZ)
        if (length < 0.001) {
            directionX = 1.0
            directionZ = 0.0
        } else {
            directionX /= length
            directionZ /= length
        }

        requests[animal] = EscapeRequest(
            playerId = player.uuid,
            targetX = animal.x + directionX * ESCAPE_DISTANCE,
            targetY = animal.y,
            targetZ = animal.z + directionZ * ESCAPE_DISTANCE,
            expiresAt = animal.level().gameTime + REQUEST_DURATION_TICKS,
        )
    }

    private fun canTriggerAvoidance(player: ServerPlayer?): Boolean =
        player != null && !player.isSpectator && !player.isCreative && !player.isShiftKeyDown

    data class EscapeRequest(
        val playerId: UUID,
        val targetX: Double,
        val targetY: Double,
        val targetZ: Double,
        val expiresAt: Long,
    )
}

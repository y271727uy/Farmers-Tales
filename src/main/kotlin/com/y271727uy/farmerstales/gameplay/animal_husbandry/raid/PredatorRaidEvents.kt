package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModSounds
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.AnimalHusbandryStatus
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdDogState
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdDogTask
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdingRules
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonEvents
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent
import net.minecraftforge.event.entity.living.LivingDeathEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Night-time predator raids on domesticated livestock. Spawned wolves and foxes
 * carry a persistent raider mark so they can be cleaned up at dawn or after a kill.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object PredatorRaidEvents {
    private val wolfHuntingCooldownUntil = WeakHashMap<ServerLevel, Long>()
    private val foxHuntingCooldownUntil = WeakHashMap<ServerLevel, Long>()
    private val wolfAbandonUntil = WeakHashMap<ServerLevel, Long>()
    private val wolfNightlyAttempts = WeakHashMap<ServerLevel, MutableMap<Long, Long>>()
    private val foxNightlyAttempts = WeakHashMap<ServerLevel, MutableMap<Long, Long>>()
    private val crimsonWolfNights = WeakHashMap<ServerLevel, CrimsonWolfNightState>()
    private val wolfForcedStay = WeakHashMap<ServerLevel, Boolean>()
    private val fenceBreaksRemaining = WeakHashMap<ServerLevel, Int>()

    fun hasFenceBreakQuota(level: ServerLevel): Boolean =
        (fenceBreaksRemaining[level] ?: 0) > 0

    fun consumeFenceBreakQuota(level: ServerLevel) {
        fenceBreaksRemaining[level] = 0
    }

    fun areWolfRaidersLeaving(level: ServerLevel): Boolean =
        isWolfHuntingCooldown(level) ||
            isWolfAbandonDue(level) ||
            (!isRaidNight(level) && wolfForcedStay[level] != true && !CrimsonMoonEvents.isActive(level))

    enum class ForceWolfRaidResult {
        SPAWNED,
        NO_FLOCK,
        FAILED,
    }

    enum class ForceFenceBreakResult {
        GRANTED,
        NO_RAIDERS,
    }

    fun forceWolfRaid(player: ServerPlayer): ForceWolfRaidResult {
        val flock = domesticSheepNear(player) ?: return ForceWolfRaidResult.NO_FLOCK
        val level = player.serverLevel()
        val spawned = spawnWolfPack(level, flockCenter(flock), flock, level.players().filterIsInstance<ServerPlayer>())
        if (spawned <= 0) return ForceWolfRaidResult.FAILED
        wolfForcedStay[level] = true
        return ForceWolfRaidResult.SPAWNED
    }

    fun forceFenceBreak(player: ServerPlayer): ForceFenceBreakResult {
        val level = player.serverLevel()
        val raiders = level.getEntitiesOfClass(
            Wolf::class.java,
            player.boundingBox.inflate(RAIDER_SCAN_RANGE),
        ) { wolf -> LivestockRaiderState.isRaider(wolf) && wolf.isAlive && !wolf.isTame }
        if (raiders.isEmpty()) return ForceFenceBreakResult.NO_RAIDERS

        fenceBreaksRemaining[level] = 1
        raiders.forEach { wolf ->
            keepRaidAnger(wolf)
            val current = wolf.target as? Sheep
            if (current != null && current.isAlive && ShepherdingRules.isEligible(current)) return@forEach
            level.getEntitiesOfClass(
                Sheep::class.java,
                wolf.boundingBox.inflate(FLOCK_SEARCH_RANGE, FLOCK_SEARCH_HEIGHT, FLOCK_SEARCH_RANGE),
            ) { sheep -> ShepherdingRules.isEligible(sheep) && sheep.isAlive }
                .minByOrNull { it.distanceToSqr(wolf) }
                ?.let { wolf.target = it }
        }
        return ForceFenceBreakResult.GRANTED
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (event.server.tickCount % Config.wolfRaidCheckIntervalTicks != 0) return

        event.server.allLevels.forEach { level ->
            cleanupRaiders(level)
            if (Config.wolfRaidEnabled) trySpawnWolfRaids(level)
            if (Config.foxRaidEnabled) trySpawnFoxRaids(level)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onLivingHurt(event: LivingHurtEvent) {
        if (event.entity.level().isClientSide) return
        val attacker = event.source.entity as? Mob ?: return
        when (val victim = event.entity) {
            is Sheep -> {
                val wolf = attacker as? Wolf ?: return
                if (wolf.isTame || !ShepherdingRules.isEligible(victim)) return
                alertHerdingDogs(victim, wolf)
            }
            is Chicken -> {
                if (attacker !is Fox || !isDomesticatedChicken(victim)) return
                alertHerdingDogs(victim, attacker)
            }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onLivingDeath(event: LivingDeathEvent) {
        if (event.entity.level().isClientSide) return
        val level = event.entity.level() as? ServerLevel ?: return
        if (CrimsonMoonEvents.isActive(level) && LivestockRaiderState.isRaider(event.entity)) {
            val player = event.source.entity as? ServerPlayer
                ?: (event.source.entity as? Wolf)?.owner as? ServerPlayer
            player?.let { FarmersTalesAdvancements.award(it, "animal_husbandry/night_watch") }
        }
        val killer = event.source.entity as? LivingEntity ?: return
        if (!LivestockRaiderState.isRaider(killer)) return
        when {
            killer is Wolf && event.entity is Sheep && ShepherdingRules.isEligible(event.entity as Sheep) -> {
                // A crimson night keeps the pack until dawn even after a kill.
                if (!CrimsonMoonEvents.isActive(level)) {
                    wolfHuntingCooldownUntil[level] = level.gameTime + Config.wolfRaidHuntingCooldownTicks
                    wolfAbandonUntil.remove(level)
                    wolfForcedStay.remove(level)
                }
            }
            killer is Fox && event.entity is Chicken && isDomesticatedChicken(event.entity as Chicken) ->
                foxHuntingCooldownUntil[level] = level.gameTime + Config.foxRaidHuntingCooldownTicks
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onBabySpawn(event: BabyEntitySpawnEvent) {
        if (isRaiderParent(event.parentA) || isRaiderParent(event.parentB)) {
            event.isCanceled = true
        }
    }

    private fun trySpawnWolfRaids(level: ServerLevel) {
        if (!canAttemptRaid(level)) return
        if (CrimsonMoonEvents.isActive(level)) {
            trySpawnCrimsonWolfWaves(level)
            return
        }
        if (isWolfHuntingCooldown(level)) return
        forEachPlayerCluster(level) { tryWolfRaidNear(it) }
    }

    private fun trySpawnCrimsonWolfWaves(level: ServerLevel) {
        val currentDay = level.dayTime / 24_000L
        val timeOfDay = level.dayTime % 24_000L
        val state = crimsonWolfNights.getOrPut(level, ::CrimsonWolfNightState)
        if (state.rolledDay != currentDay) {
            state.rolledDay = currentDay
            state.nextWave = 0
            val waveCount = CRIMSON_MIN_WAVES + level.random.nextInt(CRIMSON_WAVE_VARIANCE)
            val span = (NIGHT_END - NIGHT_START).toInt().coerceAtLeast(1)
            val slot = span / waveCount
            state.waveTimes = LongArray(waveCount) { i ->
                val slotStart = NIGHT_START + i.toLong() * slot
                val jitter = if (slot > 1) level.random.nextInt(slot) else 0
                slotStart + jitter
            }
        }

        if (state.nextWave >= state.waveTimes.size) return
        if (timeOfDay < state.waveTimes[state.nextWave]) return

        var spawned = false
        forEachPlayerCluster(level) { player ->
            val flock = domesticSheepNear(player) ?: return@forEachPlayerCluster
            val center = flockCenter(flock)
            if (hasNearbyWolfRaider(level, center)) return@forEachPlayerCluster
            if (spawnWolfPack(level, center, flock, level.players().filterIsInstance<ServerPlayer>()) > 0) {
                spawned = true
            }
        }
        if (spawned) state.nextWave++
    }

    private fun trySpawnFoxRaids(level: ServerLevel) {
        if (!canAttemptRaid(level) || isFoxHuntingCooldown(level)) return
        forEachPlayerCluster(level) { tryFoxRaidNear(it) }
    }

    private fun tryWolfRaidNear(player: ServerPlayer) {
        val level = player.serverLevel()
        val flock = domesticSheepNear(player) ?: return
        val center = flockCenter(flock)
        if (!claimNightlyAttempt(wolfNightlyAttempts, level, center.x, center.z)) return
        if (level.random.nextDouble() >= Config.wolfRaidChance) return
        if (hasNearbyWolfRaider(level, center)) return

        spawnWolfPack(level, center, flock, level.players().filterIsInstance<ServerPlayer>())
    }

    private fun domesticSheepNear(player: ServerPlayer): List<Sheep>? {
        val flock = player.serverLevel().getEntitiesOfClass(
            Sheep::class.java,
            player.boundingBox.inflate(FLOCK_SEARCH_RANGE, FLOCK_SEARCH_HEIGHT, FLOCK_SEARCH_RANGE),
        ) { sheep -> ShepherdingRules.isEligible(sheep) && sheep.isAlive }
        return flock.takeIf { it.size >= MIN_FLOCK_SIZE }
    }

    private fun tryFoxRaidNear(player: ServerPlayer) {
        val level = player.serverLevel()
        val flock = level.getEntitiesOfClass(
            Chicken::class.java,
            player.boundingBox.inflate(FLOCK_SEARCH_RANGE, FLOCK_SEARCH_HEIGHT, FLOCK_SEARCH_RANGE),
            ::isDomesticatedChicken,
        )
        if (flock.size < MIN_FLOCK_SIZE) return

        val center = flockCenter(flock)
        if (!claimNightlyAttempt(foxNightlyAttempts, level, center.x, center.z)) return
        if (level.random.nextDouble() >= Config.foxRaidChance) return
        if (hasNearbyFox(level, center)) return

        spawnFox(level, center, flock, level.players().filterIsInstance<ServerPlayer>())
    }

    private fun spawnWolfPack(level: ServerLevel, center: Vec3, flock: List<Sheep>, players: List<ServerPlayer>): Int {
        val count = MIN_PACK_SIZE + level.random.nextInt(PACK_SIZE_VARIANCE)
        var spawned = 0
        repeat(SPAWN_ATTEMPTS) {
            if (spawned >= count) return spawned
            val pos = findSpawnPos(level, center, level.random, players) ?: return@repeat
            val wolf = EntityType.WOLF.create(level) ?: return@repeat
            wolf.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F)
            wolf.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(wolf.blockPosition()),
                MobSpawnType.EVENT,
                null,
                null,
            )
            wolf.setBaby(false)
            LivestockRaiderState.mark(wolf)
            keepRaidAnger(wolf)
            if (!level.addFreshEntity(wolf)) return@repeat

            flock.minByOrNull { it.distanceToSqr(wolf) }?.let { wolf.target = it }
            if (spawned == 0) {
                wolfHuntingCooldownUntil.remove(level)
                wolfAbandonUntil[level] = level.gameTime +
                    if (CrimsonMoonEvents.isActive(level)) CRIMSON_WOLF_ABANDON_TICKS else WOLF_ABANDON_TICKS
                fenceBreaksRemaining[level] =
                    if (CrimsonMoonEvents.isActive(level) && level.random.nextDouble() < FENCE_BREAK_CHANCE) 1 else 0
                level.playSound(
                    null,
                    wolf.x,
                    wolf.y,
                    wolf.z,
                    ModSounds.WOLF_HOWL.get(),
                    SoundSource.HOSTILE,
                    2.6F,
                    0.85F + level.random.nextFloat() * 0.2F,
                )
            }
            spawned++
        }
        return spawned
    }

    private fun spawnFox(level: ServerLevel, center: Vec3, flock: List<Chicken>, players: List<ServerPlayer>) {
        repeat(SPAWN_ATTEMPTS) {
            val pos = findSpawnPos(level, center, level.random, players) ?: return@repeat
            val fox = EntityType.FOX.create(level) ?: return@repeat
            fox.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360.0F, 0.0F)
            fox.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(fox.blockPosition()),
                MobSpawnType.EVENT,
                null,
                null,
            )
            fox.setBaby(false)
            LivestockRaiderState.mark(fox)
            if (!level.addFreshEntity(fox)) return@repeat

            flock.minByOrNull { it.distanceToSqr(fox) }?.let { fox.target = it }
            return
        }
    }

    private fun cleanupRaiders(level: ServerLevel) {
        val players = level.players().filterIsInstance<ServerPlayer>()
        if (players.isEmpty()) return

        if (isWolfAbandonDue(level)) wolfForcedStay.remove(level)
        val wolfShouldLeave = areWolfRaidersLeaving(level)
        val foxShouldLeave = !isRaidNight(level) || isFoxHuntingCooldown(level)
        cleanupSpecies(level, players, Wolf::class.java, wolfShouldLeave)
        cleanupSpecies(level, players, Fox::class.java, foxShouldLeave)
    }

    private fun <T : Mob> cleanupSpecies(
        level: ServerLevel,
        players: List<ServerPlayer>,
        type: Class<T>,
        shouldLeave: Boolean,
    ) {
        val raiders = players.flatMap { player ->
            level.getEntitiesOfClass(
                type,
                player.boundingBox.inflate(RAIDER_SCAN_RANGE),
                LivestockRaiderState::canDespawn,
            )
        }.distinct()

        raiders.forEach { raider ->
            if (raider is Animal && raider.isInLove) {
                raider.resetLove()
            }
            if (raider is Wolf && !shouldLeave) keepRaidAnger(raider)
            if (!shouldLeave) return@forEach

            val nearest = players.minByOrNull { it.distanceToSqr(raider) } ?: return@forEach
            if (raider.distanceToSqr(nearest) >= DESPAWN_DISTANCE_SQR) {
                raider.discard()
                return@forEach
            }

            raider.target = null
            raider.lastHurtByMob = null
            val dx = raider.x - nearest.x
            val dz = raider.z - nearest.z
            val length = hypot(dx, dz).coerceAtLeast(0.001)
            raider.navigation.moveTo(
                raider.x + dx / length * FLEE_DISTANCE,
                raider.y,
                raider.z + dz / length * FLEE_DISTANCE,
                FLEE_SPEED,
            )
        }
    }

    private fun alertHerdingDogs(victim: LivingEntity, attacker: Mob) {
        victim.level().getEntitiesOfClass(
            Wolf::class.java,
            victim.boundingBox.inflate(DOG_ALERT_RANGE),
        ) { dog ->
            dog.isTame && !dog.isOrderedToSit && ShepherdDogState.task(dog) == ShepherdDogTask.HERDING
        }.forEach { dog ->
            dog.target = attacker
        }
    }

    private fun findSpawnPos(
        level: ServerLevel,
        center: Vec3,
        random: RandomSource,
        players: List<ServerPlayer>,
    ): Vec3? {
        val angle = random.nextDouble() * Math.PI * 2.0
        val radius = MIN_SPAWN_RADIUS + random.nextDouble() * (MAX_SPAWN_RADIUS - MIN_SPAWN_RADIUS)
        val x = center.x + cos(angle) * radius
        val z = center.z + sin(angle) * radius
        val y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z)).toDouble()
        val pos = Vec3(x, y, z)
        if (players.any { it.distanceToSqr(pos) < MIN_PLAYER_DISTANCE_SQR }) return null

        val blockPos = BlockPos.containing(pos)
        if (!level.worldBorder.isWithinBounds(blockPos)) return null
        val below = blockPos.below()
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return null
        if (!level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty) return null
        if (!level.getFluidState(blockPos).isEmpty) return null
        return pos
    }

    private fun hasNearbyWolfRaider(level: ServerLevel, center: Vec3): Boolean =
        level.getEntitiesOfClass(
            Wolf::class.java,
            AABB(center, center).inflate(EXISTING_RAIDER_RANGE, EXISTING_RAIDER_HEIGHT, EXISTING_RAIDER_RANGE),
        ) { wolf -> LivestockRaiderState.isRaider(wolf) && wolf.isAlive }
            .isNotEmpty()

    private fun hasNearbyFox(level: ServerLevel, center: Vec3): Boolean =
        level.getEntitiesOfClass(
            Fox::class.java,
            AABB(center, center).inflate(EXISTING_RAIDER_RANGE, EXISTING_RAIDER_HEIGHT, EXISTING_RAIDER_RANGE),
        ) { fox -> fox.isAlive }
            .isNotEmpty()

    private fun claimNightlyAttempt(
        attempts: WeakHashMap<ServerLevel, MutableMap<Long, Long>>,
        level: ServerLevel,
        x: Double,
        z: Double,
    ): Boolean {
        val currentDay = level.dayTime / 24_000L
        val key = cellKey(x, z)
        val cells = attempts.getOrPut(level, ::HashMap)
        if (cells[key] == currentDay) return false
        cells[key] = currentDay
        if (cells.size > MAX_TRACKED_CELLS) {
            cells.entries.removeIf { it.value < currentDay }
        }
        return true
    }

    private fun cellKey(x: Double, z: Double): Long {
        val cellX = floor(x / ATTEMPT_CELL_SIZE).toInt()
        val cellZ = floor(z / ATTEMPT_CELL_SIZE).toInt()
        return (cellX.toLong() and 0xffffffffL) or ((cellZ.toLong() and 0xffffffffL) shl 32)
    }

    private fun canAttemptRaid(level: ServerLevel): Boolean =
        level.dimensionType().hasSkyLight() &&
            level.gameRules.getBoolean(GameRules.RULE_DOMOBSPAWNING) &&
            isRaidNight(level)

    private fun forEachPlayerCluster(level: ServerLevel, action: (ServerPlayer) -> Unit) {
        val remaining = level.players()
            .filterIsInstance<ServerPlayer>()
            .filter { !it.isSpectator }
            .toMutableList()
        while (remaining.isNotEmpty()) {
            val player = remaining.removeFirst()
            remaining.removeAll { it.distanceToSqr(player) <= PLAYER_CLUSTER_RANGE_SQR }
            action(player)
        }
    }

    private fun flockCenter(animals: List<LivingEntity>): Vec3 = Vec3(
        animals.sumOf { it.x } / animals.size,
        animals.sumOf { it.y } / animals.size,
        animals.sumOf { it.z } / animals.size,
    )

    private fun isRaidNight(level: ServerLevel): Boolean {
        val timeOfDay = level.dayTime % 24_000L
        return timeOfDay in NIGHT_START..NIGHT_END
    }

    private fun isWolfHuntingCooldown(level: ServerLevel): Boolean =
        (wolfHuntingCooldownUntil[level] ?: 0L) > level.gameTime

    private fun isWolfAbandonDue(level: ServerLevel): Boolean {
        val until = wolfAbandonUntil[level] ?: return false
        return until <= level.gameTime
    }

    private fun isFoxHuntingCooldown(level: ServerLevel): Boolean =
        (foxHuntingCooldownUntil[level] ?: 0L) > level.gameTime

    private fun keepRaidAnger(wolf: Wolf) {
        wolf.remainingPersistentAngerTime = RAID_ANGER_TICKS
    }

    private fun isDomesticatedChicken(chicken: Chicken): Boolean =
        chicken.isAlive && AnimalHusbandryState.status(chicken) == AnimalHusbandryStatus.DOMESTICATED

    private fun isRaiderParent(entity: LivingEntity): Boolean =
        (entity is Wolf || entity is Fox) && LivestockRaiderState.isRaider(entity)

    private const val NIGHT_START = 13_000L
    private const val NIGHT_END = 23_000L
    private const val PLAYER_CLUSTER_RANGE_SQR = 32.0 * 32.0
    private const val FLOCK_SEARCH_RANGE = 32.0
    private const val FLOCK_SEARCH_HEIGHT = 12.0
    private const val MIN_FLOCK_SIZE = 4
    private const val MIN_PACK_SIZE = 2
    private const val PACK_SIZE_VARIANCE = 3
    private const val SPAWN_ATTEMPTS = 12
    private const val MIN_SPAWN_RADIUS = 20.0
    private const val MAX_SPAWN_RADIUS = 32.0
    private const val MIN_PLAYER_DISTANCE_SQR = 16.0 * 16.0
    private const val EXISTING_RAIDER_RANGE = 96.0
    private const val EXISTING_RAIDER_HEIGHT = 24.0
    private const val RAIDER_SCAN_RANGE = 96.0
    private const val DESPAWN_DISTANCE_SQR = 32.0 * 32.0
    private const val FLEE_DISTANCE = 16.0
    private const val FLEE_SPEED = 1.2
    private const val DOG_ALERT_RANGE = 32.0
    private const val ATTEMPT_CELL_SIZE = 32.0
    private const val MAX_TRACKED_CELLS = 1024
    private const val CRIMSON_MIN_WAVES = 2
    private const val CRIMSON_WAVE_VARIANCE = 3
    private const val WOLF_ABANDON_TICKS = 400
    private const val CRIMSON_WOLF_ABANDON_TICKS = 2_400
    private const val FENCE_BREAK_CHANCE = 0.25
    private const val RAID_ANGER_TICKS = 24_000

    private class CrimsonWolfNightState {
        var rolledDay = Long.MIN_VALUE
        var waveTimes = LongArray(0)
        var nextWave = 0
    }
}

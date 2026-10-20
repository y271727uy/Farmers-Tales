package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModSounds
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.animal.Chicken
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin

/** Dawn rooster crows, plus occasional distant night howls that do not spawn wolves. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AmbientAnimalSoundEvents {
    private const val DAWN_START = 0L
    private const val DAWN_END = 1_000L
    private const val NIGHT_START = 13_000L
    private const val NIGHT_END = 23_999L
    private const val SCAN_INTERVAL_TICKS = 20
    private const val SEARCH_RANGE = 48.0
    private const val WOLF_NIGHT_CHANCE = 0.20
    private const val FAKE_HOWL_MIN_DISTANCE = 24.0
    private const val FAKE_HOWL_MAX_DISTANCE = 48.0

    private val roosterCrowedDayByPlayer = HashMap<UUID, Long>()
    private val howlByLevel = WeakHashMap<ServerLevel, NightHowlState>()

    @JvmStatic
    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val player = event.player as? ServerPlayer ?: return
        if (player.tickCount % SCAN_INTERVAL_TICKS != 0) return
        tryRoosterCrow(player)
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if (event.server.tickCount % SCAN_INTERVAL_TICKS != 0) return
        event.server.allLevels.forEach(::tryWolfHowl)
    }

    private fun tryRoosterCrow(player: ServerPlayer) {
        val level = player.serverLevel()
        if (!level.dimensionType().hasSkyLight()) return

        val timeOfDay = level.dayTime % 24_000L
        if (timeOfDay !in DAWN_START..DAWN_END) return

        val currentDay = level.dayTime / 24_000L
        if (roosterCrowedDayByPlayer[player.uuid] == currentDay) return

        val chickens = level.getEntitiesOfClass(
            Chicken::class.java,
            player.boundingBox.inflate(SEARCH_RANGE),
        ) { chicken -> !chicken.isBaby }
        if (chickens.isEmpty()) return

        val rooster = chickens[level.random.nextInt(chickens.size)]
        level.playSound(
            null,
            rooster.x,
            rooster.y,
            rooster.z,
            ModSounds.ROOSTER_CROW.get(),
            SoundSource.NEUTRAL,
            1.0f,
            0.9f + level.random.nextFloat() * 0.2f,
        )
        val rangeSquared = SEARCH_RANGE * SEARCH_RANGE
        level.players().filterIsInstance<ServerPlayer>()
            .filter { it.distanceToSqr(rooster) <= rangeSquared }
            .forEach { roosterCrowedDayByPlayer[it.uuid] = currentDay }
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        roosterCrowedDayByPlayer.remove(event.entity.uuid)
    }

    private fun tryWolfHowl(level: ServerLevel) {
        if (!level.dimensionType().hasSkyLight()) return

        val timeOfDay = level.dayTime % 24_000L
        if (timeOfDay !in NIGHT_START..NIGHT_END) return

        val currentDay = level.dayTime / 24_000L
        val state = howlByLevel.getOrPut(level, ::NightHowlState)
        if (state.rolledDay != currentDay) {
            state.rolledDay = currentDay
            state.played = false
            state.howlTime = scheduledHowlTime(level)
        } else if (CrimsonMoonEvents.isActive(level) && !state.played) {
            // Lottery may resolve after the first night scan; dusk howl is mandatory on crimson nights.
            state.howlTime = NIGHT_START
        }

        if (state.played || state.howlTime < 0L || timeOfDay < state.howlTime) return

        val players = level.players().filterIsInstance<ServerPlayer>().filter { !it.isSpectator }
        if (players.isEmpty()) return

        val player = players[level.random.nextInt(players.size)]
        val angle = level.random.nextDouble() * Math.PI * 2.0
        val distance = FAKE_HOWL_MIN_DISTANCE +
            level.random.nextDouble() * (FAKE_HOWL_MAX_DISTANCE - FAKE_HOWL_MIN_DISTANCE)
        level.playSound(
            null,
            player.x + cos(angle) * distance,
            player.y,
            player.z + sin(angle) * distance,
            ModSounds.WOLF_HOWL.get(),
            SoundSource.NEUTRAL,
            1.8f,
            0.85f + level.random.nextFloat() * 0.2f,
        )
        state.played = true
    }

    private fun scheduledHowlTime(level: ServerLevel): Long {
        if (CrimsonMoonEvents.isActive(level)) return NIGHT_START
        if (level.random.nextDouble() >= WOLF_NIGHT_CHANCE) return -1L
        val nightLength = (NIGHT_END - NIGHT_START + 1L).toInt()
        return NIGHT_START + level.random.nextInt(nightLength)
    }

    private class NightHowlState {
        var rolledDay = Long.MIN_VALUE
        var howlTime = -1L
        var played = false
    }
}

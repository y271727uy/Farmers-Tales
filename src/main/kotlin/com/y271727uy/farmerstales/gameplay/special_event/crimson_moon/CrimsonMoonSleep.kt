package com.y271727uy.farmerstales.gameplay.special_event.crimson_moon

import com.mojang.datafixers.util.Either
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModSounds
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdingRules
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseClientState
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Unit as McUnit
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.level.SleepFinishedTimeEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.UUID
import java.util.WeakHashMap

/** First bed click warns; the second lets vanilla sleep proceed. Sleeping itself is stretched. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object CrimsonMoonSleep {
    private const val CONFIRM_MESSAGE = "message.${FTMod.MODID}.crimson_moon.sleep_confirm"
    private const val SLEEP_SPARED_MESSAGE = "message.${FTMod.MODID}.crimson_moon.sleep_spared"
    private const val SLEEP_SHEEP_LOST_MESSAGE = "message.${FTMod.MODID}.crimson_moon.sleep_sheep_lost"
    private const val HOWL_CHANCE = 0.5
    private const val STRETCH_PERIOD = 5
    private const val STRETCH_SKIP = 2
    private const val SLEEP_PENALTY_CHANCE = 0.5
    private const val MIN_SHEEP_LOST = 1
    private const val MAX_SHEEP_LOST = 4
    private const val SHEEP_SEARCH_RANGE = 32.0
    private const val SHEEP_SEARCH_HEIGHT = 12.0

    private val confirmByPlayer = HashMap<UUID, ConfirmState>()
    private val settledPenaltyDays = WeakHashMap<ServerLevel, MutableSet<Long>>()

    @JvmStatic
    fun tryWarnFirstAttempt(player: ServerPlayer): Either<Player.BedSleepingProblem, McUnit>? {
        if (!CrimsonMoonEvents.isActive(player.serverLevel())) return null
        val day = player.serverLevel().dayTime / 24_000L
        val state = confirmByPlayer.getOrPut(player.uuid, ::ConfirmState)
        if (state.day != day) {
            state.day = day
            state.confirmed = false
        }
        if (state.confirmed) return null

        state.confirmed = true
        player.displayClientMessage(Component.translatable(CONFIRM_MESSAGE), true)
        if (player.random.nextDouble() < HOWL_CHANCE) {
            playHowl(player)
        }
        return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM)
    }

    @JvmStatic
    fun shouldSkipSleepTick(player: Player): Boolean {
        if (!player.isSleeping || !isCrimsonMoonVisual(player)) return false
        return Math.floorMod(player.tickCount, STRETCH_PERIOD) < STRETCH_SKIP
    }

    @JvmStatic
    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        confirmByPlayer.remove(event.entity.uuid)
    }

    /** 50/50: skip a crimson-moon night quietly, or lose 1–4 nearby domesticated sheep. */
    @JvmStatic
    @SubscribeEvent
    fun onSleepFinished(event: SleepFinishedTimeEvent) {
        val level = event.level as? ServerLevel ?: return
        if (!CrimsonMoonEvents.isActive(level)) return
        if (!skipsToDay(event.newTime)) return

        val nightDay = level.dayTime / 24_000L
        val settled = settledPenaltyDays.getOrPut(level, ::HashSet)
        if (!settled.add(nightDay)) return
        settled.removeIf { it < nightDay }

        val players = level.players().filterIsInstance<ServerPlayer>()
        if (players.isEmpty()) return
        val sleepers = players.filter { it.isSleeping }
        val sources = sleepers.ifEmpty { players }

        if (level.random.nextDouble() >= SLEEP_PENALTY_CHANCE) {
            notifyPlayers(players, Component.translatable(SLEEP_SPARED_MESSAGE))
            return
        }

        val lost = killNearbyDomesticSheep(level, sources)
        if (lost <= 0) {
            notifyPlayers(players, Component.translatable(SLEEP_SPARED_MESSAGE))
            return
        }
        notifyPlayers(players, Component.translatable(SLEEP_SHEEP_LOST_MESSAGE, lost))
    }

    private fun isCrimsonMoonVisual(player: Player): Boolean {
        val level = player.level()
        return if (level is ServerLevel) {
            CrimsonMoonEvents.isActive(level)
        } else {
            MoonPhaseClientState.winnerId == CrimsonMoonEvents.ID
        }
    }

    private fun skipsToDay(newTime: Long): Boolean {
        val timeOfDay = newTime % 24_000L
        return timeOfDay !in 13_000L..23_999L
    }

    private fun killNearbyDomesticSheep(level: ServerLevel, sources: List<ServerPlayer>): Int {
        val pool = LinkedHashSet<Sheep>()
        sources.forEach { player ->
            pool.addAll(
                level.getEntitiesOfClass(
                    Sheep::class.java,
                    player.boundingBox.inflate(SHEEP_SEARCH_RANGE, SHEEP_SEARCH_HEIGHT, SHEEP_SEARCH_RANGE),
                ) { sheep -> ShepherdingRules.isEligible(sheep) && sheep.isAlive },
            )
        }
        if (pool.isEmpty()) return 0

        val remaining = ArrayList(pool)
        val wanted = level.random.nextInt(MIN_SHEEP_LOST, MAX_SHEEP_LOST + 1)
        val lost = minOf(wanted, remaining.size)
        repeat(lost) {
            val sheep = remaining.removeAt(level.random.nextInt(remaining.size))
            sheep.hurt(level.damageSources().generic(), Float.MAX_VALUE)
        }
        return lost
    }

    private fun notifyPlayers(players: List<ServerPlayer>, message: Component) {
        players.forEach { it.sendSystemMessage(message) }
    }

    private fun playHowl(player: ServerPlayer) {
        val level = player.serverLevel()
        level.playSound(
            null,
            player.x,
            player.y,
            player.z,
            ModSounds.WOLF_HOWL.get(),
            SoundSource.NEUTRAL,
            1.4f,
            0.85f + level.random.nextFloat() * 0.2f,
        )
    }

    private class ConfirmState {
        var day = Long.MIN_VALUE
        var confirmed = false
    }
}

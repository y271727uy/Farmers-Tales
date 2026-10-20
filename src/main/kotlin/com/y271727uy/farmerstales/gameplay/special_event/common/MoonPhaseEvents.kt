package com.y271727uy.farmerstales.gameplay.special_event.common

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import com.y271727uy.farmerstales.network.MoonPhaseSyncPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor
import java.util.Locale
import java.util.function.Supplier

/**
 * Shared per-dimension moon-event lottery. Eligible events on a night are
 * drawn once; at most one winner is active.
 *
 * The world winner is synced on its own packet. The marker effect is HUD-only
 * and is reapplied if something strips it mid-night.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object MoonPhaseEvents {
    private const val NIGHT_START = 13_000L
    private const val NIGHT_END = 23_999L
    private const val DATA_NAME = "${FTMod.MODID}_moon_phase_lottery"
    private const val WINNER_ID_TAG = "WinnerId"
    private const val ROLLED_DAY_TAG = "RolledDay"
    private const val FORCED_TAG = "Forced"
    private const val FORCED_DAY_TAG = "ForcedDay"
    private const val NONE_ID = ""

    private val events = linkedMapOf<String, MoonPhaseEvent>()

    fun register(event: MoonPhaseEvent) {
        check(events.put(event.id, event) == null) { "Duplicate moon-phase event: ${event.id}" }
    }

    fun contains(id: String): Boolean = id in events

    fun ids(): Set<String> = events.keys

    fun get(id: String): MoonPhaseEvent? = events[id]

    fun isActive(id: String, level: ServerLevel): Boolean {
        val event = events[id] ?: return false
        return isActiveNow(event, level)
    }

    /** Client-side: the event whose marker effect is currently on the player. */
    fun clientActiveEvent(player: Player): MoonPhaseEvent? =
        events.values.firstOrNull { player.hasEffect(it.effect()) }

    /** Debug-only override: makes the current day's night fire this event, without changing the moon phase. */
    fun forceForCurrentNight(id: String, level: ServerLevel) {
        if (id !in events) return
        val state = data(level)
        val currentDay = level.dayTime / 24_000L
        state.winnerId = id
        state.forced = true
        state.forcedDay = currentDay
        state.rolledDay = currentDay
        state.setDirty()
        if (level.dayTime % 24_000L in NIGHT_START..NIGHT_END) {
            applyToAll(level, state, currentDay)
        } else {
            state.appliedDay = Long.MIN_VALUE
            syncAll(level)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        event.server.allLevels.forEach(::tickLevel)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    private fun tickLevel(level: ServerLevel) {
        val time = level.dayTime % 24_000L
        val state = data(level)
        val currentDay = level.dayTime / 24_000L

        if (time !in NIGHT_START..NIGHT_END) {
            if (state.winnerId.isNotEmpty() && (!state.forced || state.forcedDay != currentDay)) {
                state.winnerId = NONE_ID
                state.forced = false
                state.appliedDay = Long.MIN_VALUE
                state.setDirty()
                syncAll(level)
            }
            return
        }

        if (state.forced && state.forcedDay == currentDay) {
            ensureNight(level, state, currentDay)
            return
        }

        if (state.rolledDay == currentDay) {
            ensureNight(level, state, currentDay)
            return
        }

        val winner = rollWinner(level)
        state.rolledDay = currentDay
        state.forced = false
        state.winnerId = winner?.id ?: NONE_ID
        state.setDirty()
        if (winner != null) {
            level.players().filterIsInstance<ServerPlayer>().forEach { player ->
                player.sendSystemMessage(Component.translatable(winner.startedMessage))
            }
            applyToAll(level, state, currentDay)
        } else {
            syncAll(level)
        }
    }

    private fun rollWinner(level: ServerLevel): MoonPhaseEvent? {
        val candidates = events.values.filter { event ->
            event.chance > 0.0 && matchesPhase(event, level) && matchesSeason(event, level)
        }
        if (candidates.isEmpty()) return null

        val weightSum = candidates.sumOf { it.chance }
        val noneWeight = (1.0 - weightSum).coerceAtLeast(0.0)
        val total = weightSum + noneWeight
        if (total <= 0.0) return null

        var cursor = level.random.nextDouble() * total
        for (candidate in candidates) {
            cursor -= candidate.chance
            if (cursor < 0.0) return candidate
        }
        return if (noneWeight <= 0.0) candidates.last() else null
    }

    private fun ensureNight(level: ServerLevel, state: MoonPhaseLotteryData, currentDay: Long) {
        if (state.winnerId.isEmpty()) return
        if (state.appliedDay != currentDay) {
            applyToAll(level, state, currentDay)
        } else {
            ensureHud(level)
        }
    }

    private fun applyToAll(level: ServerLevel, state: MoonPhaseLotteryData, currentDay: Long) {
        state.appliedDay = currentDay
        syncAll(level)
    }

    private fun syncAll(level: ServerLevel) {
        val id = currentWinnerId(level)
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.DIMENSION.with(Supplier { level.dimension() }),
            MoonPhaseSyncPacket(id),
        )
        level.players().filterIsInstance<ServerPlayer>().forEach { applyHud(it, id) }
    }

    private fun syncPlayer(player: ServerPlayer) {
        val id = currentWinnerId(player.serverLevel())
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(Supplier { player }),
            MoonPhaseSyncPacket(id),
        )
        applyHud(player, id)
    }

    private fun ensureHud(level: ServerLevel) {
        val id = currentWinnerId(level)
        if (id.isEmpty()) return
        level.players().filterIsInstance<ServerPlayer>().forEach { applyHud(it, id) }
    }

    private fun applyHud(player: ServerPlayer, winnerId: String) {
        events.values.forEach { event ->
            val effect = event.effect()
            if (event.id == winnerId) {
                if (!player.hasEffect(effect)) {
                    player.addEffect(
                        MobEffectInstance(effect, remainingNightTicks(player.serverLevel()), 0, false, true, true),
                    )
                }
            } else if (player.hasEffect(effect)) {
                player.removeEffect(effect)
            }
        }
    }

    private fun currentWinnerId(level: ServerLevel): String {
        val event = events[data(level).winnerId] ?: return NONE_ID
        return if (isActiveNow(event, level)) event.id else NONE_ID
    }

    private fun remainingNightTicks(level: ServerLevel): Int {
        val time = level.dayTime % 24_000L
        return (NIGHT_END - time + 1L).coerceIn(1L, NIGHT_END - NIGHT_START + 1L).toInt()
    }

    private fun isActiveNow(event: MoonPhaseEvent, level: ServerLevel): Boolean {
        val time = level.dayTime % 24_000L
        if (time !in NIGHT_START..NIGHT_END) return false
        val state = data(level)
        if (state.winnerId != event.id) return false
        return state.forced || (matchesPhase(event, level) && matchesSeason(event, level))
    }

    private fun matchesPhase(event: MoonPhaseEvent, level: ServerLevel): Boolean =
        level.moonPhase == event.phase.id

    private fun matchesSeason(event: MoonPhaseEvent, level: ServerLevel): Boolean {
        val season = event.season ?: return true
        if (!IntegrationManager.isSereneSeasonsLoaded()) return true
        return IntegrationManager.currentSeasonName(level) == season.name.lowercase(Locale.ROOT)
    }

    private fun data(level: ServerLevel): MoonPhaseLotteryData =
        level.dataStorage.computeIfAbsent(::load, ::MoonPhaseLotteryData, DATA_NAME)

    private fun load(tag: CompoundTag): MoonPhaseLotteryData = MoonPhaseLotteryData().apply {
        winnerId = tag.getString(WINNER_ID_TAG)
        rolledDay = if (tag.contains(ROLLED_DAY_TAG)) tag.getLong(ROLLED_DAY_TAG) else Long.MIN_VALUE
        forced = tag.getBoolean(FORCED_TAG)
        forcedDay = if (tag.contains(FORCED_DAY_TAG)) tag.getLong(FORCED_DAY_TAG) else Long.MIN_VALUE
    }

    private class MoonPhaseLotteryData : SavedData() {
        var winnerId = NONE_ID
        var rolledDay = Long.MIN_VALUE
        var forced = false
        var forcedDay = Long.MIN_VALUE
        /** In-memory only: the day whose night packet was already sent to online players. */
        var appliedDay = Long.MIN_VALUE

        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putString(WINNER_ID_TAG, winnerId)
            putLong(ROLLED_DAY_TAG, rolledDay)
            putBoolean(FORCED_TAG, forced)
            putLong(FORCED_DAY_TAG, forcedDay)
        }
    }
}

package com.y271727uy.farmerstales.gameplay.special_event.common

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.DayClock
import com.y271727uy.farmerstales.gameplay.common.PlayerLevelSync
import com.y271727uy.farmerstales.gameplay.common.announce
import com.y271727uy.farmerstales.gameplay.common.dayIndex
import com.y271727uy.farmerstales.gameplay.common.isNightTime
import com.y271727uy.farmerstales.gameplay.common.sendPacket
import com.y271727uy.farmerstales.gameplay.common.sendToDimension
import com.y271727uy.farmerstales.gameplay.common.tickOfDay
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.network.MoonPhaseSyncPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.level.LevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Locale
import java.util.WeakHashMap

/**
 * Shared per-dimension moon-event lottery. Tonight's result is drawn once at dawn;
 * effects and the start announcement remain gated to the night.
 *
 * The world winner is synced on its own packet. The marker effect is HUD-only
 * and is reapplied if something strips it mid-night.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object MoonPhaseEvents {
    private const val DATA_NAME = "${FTMod.MODID}_moon_phase_lottery"
    private const val WINNER_ID_TAG = "WinnerId"
    private const val ROLLED_DAY_TAG = "RolledDay"
    private const val FORCED_TAG = "Forced"
    private const val FORCED_DAY_TAG = "ForcedDay"
    private const val ANNOUNCED_DAY_TAG = "AnnouncedDay"
    private const val NONE_ID = ""

    private val events = linkedMapOf<String, MoonPhaseEvent>()
    private val cache = WeakHashMap<ServerLevel, MoonPhaseLotteryData>()

    init {
        PlayerLevelSync.register(::syncPlayer)
    }

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
        val currentDay = level.dayIndex
        if (state.rolledDay != currentDay || state.winnerId != id) state.announcedDay = Long.MIN_VALUE
        state.winnerId = id
        state.forced = true
        state.forcedDay = currentDay
        state.rolledDay = currentDay
        state.setDirty()
        if (level.isNightTime) {
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

    private fun tickLevel(level: ServerLevel) {
        val state = data(level)
        val currentDay = level.dayIndex
        val changed = rollForDay(level, state, currentDay)

        if (!level.isNightTime) {
            if (changed || state.appliedDay != Long.MIN_VALUE) {
                state.appliedDay = Long.MIN_VALUE
                syncAll(level)
            }
            return
        }

        ensureNight(level, state, currentDay)
    }

    private fun rollForDay(level: ServerLevel, state: MoonPhaseLotteryData, currentDay: Long): Boolean {
        if (state.rolledDay == currentDay) return false
        val winner = rollWinner(level)
        state.rolledDay = currentDay
        state.forced = false
        state.winnerId = winner?.id ?: NONE_ID
        state.appliedDay = Long.MIN_VALUE
        state.setDirty()
        return true
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
        if (state.appliedDay != currentDay) {
            applyToAll(level, state, currentDay)
        } else if (level.gameTime % 20L == 0L) {
            ensureHud(level)
        }
    }

    private fun applyToAll(level: ServerLevel, state: MoonPhaseLotteryData, currentDay: Long) {
        state.appliedDay = currentDay
        val winner = events[currentWinnerId(level)]
        if (winner != null && state.announcedDay != currentDay) {
            state.announcedDay = currentDay
            state.setDirty()
            level.announce(Component.translatable(winner.startedMessage))
        }
        syncAll(level)
    }

    private fun syncAll(level: ServerLevel) {
        val id = currentWinnerId(level)
        level.sendToDimension(packet(level, id))
        level.players().forEach { applyHud(it, id) }
    }

    private fun syncPlayer(player: ServerPlayer) {
        val level = player.serverLevel()
        val state = data(level)
        if (rollForDay(level, state, level.dayIndex)) {
            // Also update anyone already here if a login arrives before the first tick of a new day.
            syncAll(level)
            return
        }
        val id = currentWinnerId(level)
        player.sendPacket(packet(level, id))
        applyHud(player, id)
    }

    private fun packet(level: ServerLevel, activeId: String): MoonPhaseSyncPacket {
        val state = data(level)
        val forecastId = state.winnerId.takeIf { it in events } ?: NONE_ID
        return MoonPhaseSyncPacket(activeId, forecastId, state.rolledDay)
    }

    private fun ensureHud(level: ServerLevel) {
        val id = currentWinnerId(level)
        if (id.isEmpty()) return
        level.players().forEach { applyHud(it, id) }
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
        return (DayClock.NIGHT_END - level.tickOfDay + 1L)
            .coerceIn(1L, DayClock.NIGHT_END - DayClock.NIGHT_START + 1L).toInt()
    }

    private fun isActiveNow(event: MoonPhaseEvent, level: ServerLevel): Boolean {
        if (!level.isNightTime) return false
        val state = data(level)
        // Eligibility was decided when this day's forecast was drawn. Never re-roll at night.
        return state.rolledDay == level.dayIndex && state.winnerId == event.id
    }

    private fun matchesPhase(event: MoonPhaseEvent, level: ServerLevel): Boolean =
        level.moonPhase == event.phase.id

    private fun matchesSeason(event: MoonPhaseEvent, level: ServerLevel): Boolean {
        val season = event.season ?: return true
        if (!IntegrationManager.isSereneSeasonsLoaded()) return true
        return IntegrationManager.currentSeasonName(level) == season.name.lowercase(Locale.ROOT)
    }

    private fun data(level: ServerLevel): MoonPhaseLotteryData = cache.getOrPut(level) {
        level.dataStorage.computeIfAbsent(::load, ::MoonPhaseLotteryData, DATA_NAME)
    }

    @JvmStatic @SubscribeEvent
    fun onUnload(event: LevelEvent.Unload) { (event.level as? ServerLevel)?.let { cache.remove(it) } }

    private fun load(tag: CompoundTag): MoonPhaseLotteryData = MoonPhaseLotteryData().apply {
        winnerId = tag.getString(WINNER_ID_TAG)
        rolledDay = if (tag.contains(ROLLED_DAY_TAG)) tag.getLong(ROLLED_DAY_TAG) else Long.MIN_VALUE
        forced = tag.getBoolean(FORCED_TAG)
        forcedDay = if (tag.contains(FORCED_DAY_TAG)) tag.getLong(FORCED_DAY_TAG) else Long.MIN_VALUE
        // Old saves drew natural winners only after announcing them at night.
        announcedDay = if (tag.contains(ANNOUNCED_DAY_TAG)) tag.getLong(ANNOUNCED_DAY_TAG)
            else if (!forced) rolledDay else Long.MIN_VALUE
    }

    private class MoonPhaseLotteryData : SavedData() {
        var winnerId = NONE_ID
        var rolledDay = Long.MIN_VALUE
        var forced = false
        var forcedDay = Long.MIN_VALUE
        var announcedDay = Long.MIN_VALUE
        /** In-memory only: the day whose night packet was already sent to online players. */
        var appliedDay = Long.MIN_VALUE

        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putString(WINNER_ID_TAG, winnerId)
            putLong(ROLLED_DAY_TAG, rolledDay)
            putBoolean(FORCED_TAG, forced)
            putLong(FORCED_DAY_TAG, forcedDay)
            putLong(ANNOUNCED_DAY_TAG, announcedDay)
        }
    }
}

package com.y271727uy.farmerstales.gameplay.special_event.seasonal

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.DayClock
import com.y271727uy.farmerstales.gameplay.common.announce
import com.y271727uy.farmerstales.gameplay.common.dayIndex
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.SeasonCalendarDate
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Random
import java.util.WeakHashMap

enum class SeasonalEvent(val id: String, val color: ChatFormatting, internal val salt: Long) {
    HARVEST_DAY("harvest_day", ChatFormatting.GOLD, 191_723L),
    FISH_TIDE("fish_tide", ChatFormatting.AQUA, 481_249L),
    WARM_CURRENT("warm_current", ChatFormatting.YELLOW, 739_643L);

    fun displayName(): Component =
        Component.translatable("event.${FTMod.MODID}.$id").withStyle(color)
}

/** A world-specific annual calendar based on the Serene Seasons date. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object SeasonalEvents {
    private const val MONTHS_PER_SEASON = 3
    private const val KEY = "commands.${FTMod.MODID}.seasonal_events"

    private data class CalendarDay(val season: Int, val day: Int, val daysInSeason: Int)

    private val lastAnnouncementDate = WeakHashMap<ServerLevel, CalendarDay>()

    @JvmStatic
    fun isActive(level: ServerLevel, event: SeasonalEvent): Boolean =
        isActive(level, event, 0L)

    /** The pack's crab trap settles production by one-based world day, including catch-up days. */
    fun crabTrapMultiplier(level: ServerLevel, worldDay: Long): Int {
        val offset = (worldDay - 1L - level.dayIndex) * DayClock.TICKS_PER_DAY
        return if (isActive(level, SeasonalEvent.FISH_TIDE, offset)) 2 else 1
    }

    private fun isActive(level: ServerLevel, event: SeasonalEvent, offset: Long): Boolean =
        calendarDay(level, offset)?.let { date -> eventDay(level, date, event) == date.day } ?: false

    private fun calendarDay(level: ServerLevel, offset: Long = 0L): CalendarDay? {
        if (level.dimension() != Level.OVERWORLD) return null
        return IntegrationManager.calendarAt(level, offset)?.toCalendarDay()
    }

    private fun SeasonCalendarDate.toCalendarDay() = CalendarDay(
        season = monthIndex / MONTHS_PER_SEASON,
        day = monthIndex % MONTHS_PER_SEASON * daysInMonth + dayOfMonth,
        daysInSeason = daysInMonth * MONTHS_PER_SEASON,
    )

    private fun eventDay(level: ServerLevel, date: CalendarDay, event: SeasonalEvent): Int {
        val seed = level.seed xor (event.salt * (date.season + 1L))
        return 1 + Random(seed).nextInt(date.daysInSeason)
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val level = event.server.overworld()
        if (level.gameTime % 20L != 0L || level.players().isEmpty()) return
        val date = IntegrationManager.calendarAt(level, 0L) ?: return
        val today = date.toCalendarDay()
        if (lastAnnouncementDate.put(level, today) == today) return
        val tomorrow = calendarDay(level, date.dayDuration.toLong()) ?: return
        for (seasonalEvent in SeasonalEvent.entries) {
            if (eventDay(level, today, seasonalEvent) == today.day) {
                level.announce(Component.translatable("$KEY.today", seasonalEvent.displayName()))
            }
            if (eventDay(level, tomorrow, seasonalEvent) == tomorrow.day) {
                level.announce(Component.translatable("$KEY.tomorrow", seasonalEvent.displayName()))
            }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun registerCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(Commands.literal(FTMod.MODID).then(
            Commands.literal("seasonal_events").executes { context ->
                val source = context.source
                val level = source.server.overworld()
                val today = calendarDay(level)
                if (today == null) {
                    source.sendFailure(Component.translatable("$KEY.unavailable"))
                    return@executes 0
                }
                val season = SeasonWindow.entries[today.season].displayName()
                source.sendSuccess({ Component.translatable("$KEY.header", season, today.day, today.daysInSeason) }, false)
                SeasonalEvent.entries.map { it to eventDay(level, today, it) }.sortedBy { it.second }.forEach { (entry, day) ->
                    val status = when {
                        day == today.day -> "active"
                        day < today.day -> "passed"
                        else -> "upcoming"
                    }
                    source.sendSuccess({ Component.translatable("$KEY.$status", day, entry.displayName()) }, false)
                }
                1
            },
        ))
    }
}

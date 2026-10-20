package com.y271727uy.farmerstales.integration.sereneseasons

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.season_announcement.SeasonAnnouncementEvents
import com.y271727uy.farmerstales.gameplay.tropical_climate.TropicalClimateEvents
import com.y271727uy.farmerstales.integration.SeasonCalendarDate
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import sereneseasons.api.season.SeasonHelper
import java.util.EnumSet
import java.util.Locale

object SereneSeasonsIntegration {
    private const val START_YEAR = 1000L
    private const val MONTHS_IN_YEAR = 12

    fun init() {
        FTMod.LOGGER.debug("Serene Seasons compatibility enabled")
        SeasonAnnouncementEvents.register()
        TropicalClimateEvents.register()
    }

    fun currentSeasonName(level: Level): String {
        return SeasonHelper.getSeasonState(level).season.name.lowercase(Locale.ROOT)
    }

    /**
     * Calendar at a future (or past) game-time offset from now.
     * Season ticks wrap per cycle, so the offset is applied to the unwrapped
     * season clock rather than treating [net.minecraft.world.level.Level.getGameTime]
     * as season time.
     *
     * Twelve sub-seasons map onto January–December, starting at Early Spring.
     */
    fun calendarAt(level: Level, gameTimeOffset: Long): SeasonCalendarDate? {
        val state = SeasonHelper.getSeasonState(level) ?: return null
        val dayDuration = state.dayDuration.coerceAtLeast(1)
        val subSeasonDuration = state.subSeasonDuration.toLong().coerceAtLeast(1L)
        val cycleDuration = state.cycleDuration.toLong().coerceAtLeast(1L)
        val unwrapped = state.seasonCycleTicks.toLong() + gameTimeOffset

        val year = START_YEAR + Math.floorDiv(unwrapped, cycleDuration)
        val ticksInCycle = Math.floorMod(unwrapped, cycleDuration)
        val monthIndex = (ticksInCycle / subSeasonDuration).toInt().coerceIn(0, MONTHS_IN_YEAR - 1)
        val ticksInMonth = ticksInCycle % subSeasonDuration
        val dayOfMonth = 1 + (ticksInMonth / dayDuration).toInt()
        val ticksInDay = ticksInMonth % dayDuration
        val ticksPerHour = (dayDuration / 24).coerceAtLeast(1)
        val hour = ((ticksInDay / ticksPerHour) % 24).toInt()
        val minute = ((ticksInDay % ticksPerHour) / (ticksPerHour / 60f)).toInt()
        val daysInMonth = (subSeasonDuration / dayDuration).toInt().coerceAtLeast(1)

        return SeasonCalendarDate(
            hour = hour,
            minute = minute,
            monthIndex = monthIndex,
            dayOfMonth = dayOfMonth,
            year = year,
            daysInMonth = daysInMonth,
            dayDuration = dayDuration,
        )
    }
}

object SeasonSupport {
    fun currentSeasonWindow(level: Level): SeasonWindow? {
        val state = SeasonHelper.getSeasonState(level) ?: return null
        return when (state.subSeason) {
            sereneseasons.api.season.Season.SubSeason.EARLY_SPRING,
            sereneseasons.api.season.Season.SubSeason.MID_SPRING,
            sereneseasons.api.season.Season.SubSeason.LATE_SPRING -> SeasonWindow.SPRING
            sereneseasons.api.season.Season.SubSeason.EARLY_SUMMER,
            sereneseasons.api.season.Season.SubSeason.MID_SUMMER,
            sereneseasons.api.season.Season.SubSeason.LATE_SUMMER -> SeasonWindow.SUMMER
            sereneseasons.api.season.Season.SubSeason.EARLY_AUTUMN,
            sereneseasons.api.season.Season.SubSeason.MID_AUTUMN,
            sereneseasons.api.season.Season.SubSeason.LATE_AUTUMN -> SeasonWindow.AUTUMN
            sereneseasons.api.season.Season.SubSeason.EARLY_WINTER,
            sereneseasons.api.season.Season.SubSeason.MID_WINTER,
            sereneseasons.api.season.Season.SubSeason.LATE_WINTER -> SeasonWindow.WINTER
        }
    }

    fun formatSeasonsInlineOrYearRound(seasons: Collection<SeasonWindow>): Component {
        if (seasons.isEmpty() || isYearRound(seasons)) {
            return Component.translatable("season.${FTMod.MODID}.year_round").withStyle(ChatFormatting.LIGHT_PURPLE)
        }

        val result: MutableComponent = Component.empty()
        orderedSeasons(seasons).forEachIndexed { index, season ->
            if (index > 0) {
                result.append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
            }
            result.append(season.displayName())
        }
        return result
    }

    fun createSeasonInfoLines(translationKey: String, seasons: Collection<SeasonWindow>): List<Component> {
        if (seasons.isEmpty() || isYearRound(seasons)) {
            return listOf(Component.translatable(translationKey, formatSeasonsInlineOrYearRound(seasons)).withStyle(ChatFormatting.GRAY))
        }

        return buildList {
            add(Component.translatable(translationKey, Component.empty()).withStyle(ChatFormatting.GRAY))
            addAll(orderedSeasons(seasons).map(SeasonWindow::displayName))
        }
    }

    fun formatResourceLocationsInlineOrAny(locations: Collection<ResourceLocation>, prefix: String = ""): Component {
        if (locations.isEmpty()) {
            return Component.translatable("season.${FTMod.MODID}.any").withStyle(ChatFormatting.LIGHT_PURPLE)
        }

        val result: MutableComponent = Component.empty()
        locations.forEachIndexed { index, location ->
            if (index > 0) {
                result.append(Component.literal(", ").withStyle(ChatFormatting.GRAY))
            }
            result.append(Component.literal(prefix + location))
        }
        return result
    }

    fun sendPlayerFeedback(player: Player, message: Component) {
        if (!Config.sendActionBarFeedback || player.level().isClientSide) {
            return
        }
        player.displayClientMessage(message.copy().withStyle(ChatFormatting.GOLD), true)
    }

    private fun isYearRound(seasons: Collection<SeasonWindow>): Boolean = seasons.containsAll(EnumSet.allOf(SeasonWindow::class.java))

    private fun orderedSeasons(seasons: Collection<SeasonWindow>): List<SeasonWindow> =
        SeasonWindow.entries.filter(seasons::contains)

    enum class SeasonWindow {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER;

        fun displayName(): Component = Component.translatable("season.${FTMod.MODID}.${name.lowercase(Locale.ROOT)}")
            .withStyle(chatFormatting())

        fun chatFormatting(): ChatFormatting = when (this) {
            SPRING -> ChatFormatting.GREEN
            SUMMER -> ChatFormatting.YELLOW
            AUTUMN -> ChatFormatting.GOLD
            WINTER -> ChatFormatting.AQUA
        }
    }
}

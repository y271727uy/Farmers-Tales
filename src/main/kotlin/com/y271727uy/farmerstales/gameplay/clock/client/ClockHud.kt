package com.y271727uy.farmerstales.gameplay.clock.client

import com.y271727uy.farmerstales.gameplay.common.dayIndex
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhase
import com.y271727uy.farmerstales.gameplay.daylight.SeasonalDaylight
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseClientState
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseEvents
import com.y271727uy.farmerstales.gameplay.weather.precipitation.BiomePrecipitation
import com.y271727uy.farmerstales.gameplay.weather.area_weather.AreaWeatherClientState
import com.y271727uy.farmerstales.gameplay.weather.area_weather.AreaWeatherEvents
import com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus.DarkCumulonimbusClientState
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainClientState
import com.y271727uy.farmerstales.gameplay.weather.heavy_rain.HeavyRainClientState
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.mixin.minecraft.client.BiomeTemperatureAccessor
import net.minecraft.client.Minecraft
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import java.util.Locale
import kotlin.math.roundToInt

object ClockHud {
    private val environmentIconStyle = Style.EMPTY
        .withFont(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "clock"))
        .withColor(0xFFFFFF)

    fun environmentLines(level: Level, pos: BlockPos): List<Component> {
        val holder = level.getBiome(pos)
        val biome = holder.value()
        val biomeName = holder.unwrapKey().map<Component> {
            Component.translatable(it.location().toLanguageKey("biome"))
        }.orElseGet { Component.translatable("hud.${FTMod.MODID}.unknown_biome") }
        val fahrenheit = (biome as BiomeTemperatureAccessor).`farmerstales$getTemperature`(pos) * 100.0
        val celsius = (fahrenheit - 32.0) / 1.8
        val humidity = (biome.modifiedClimateSettings.downfall() * 100.0F).roundToInt()
        return listOf(
            environmentLine('\uE101', Component.translatable("hud.${FTMod.MODID}.biome", biomeName)),
            environmentLine('\uE102', Component.translatable(
                "hud.${FTMod.MODID}.temperature", String.format(Locale.ROOT, "%.2f", fahrenheit),
                celsius.roundToInt(), (celsius + 273.15).roundToInt()
            )),
            environmentLine('\uE103', Component.translatable("hud.${FTMod.MODID}.humidity", humidity)),
            environmentLine('\uE106', Component.translatable(
                "hud.${FTMod.MODID}.precipitation", BiomePrecipitation.annualRange(biome).display()
            ))
        )
    }

    private fun environmentLine(icon: Char, text: Component): Component = Component.empty()
        .append(Component.literal(icon.toString()).setStyle(environmentIconStyle)).append(" ").append(text)

    // Reuse the eight fixed lines instead of allocating new components every rendered frame.
    private val moonPhaseLines = MoonPhase.entries.map { phase ->
        environmentLine('\uE105', Component.translatable(
            "hud.${FTMod.MODID}.moon_phase",
            Component.translatable("moon_phase.${FTMod.MODID}.${phase.name.lowercase(Locale.ROOT)}")
        ))
    }
    private val specialMoonPhaseLines = mutableMapOf<String, Component>()
    private val unavailableMoonPhaseLine = environmentLine('\uE105', Component.translatable(
        "hud.${FTMod.MODID}.moon_phase", Component.translatable("hud.${FTMod.MODID}.weather.unavailable")
    ))

    fun moonPhaseLine(level: Level, player: Player): Component? {
        if (!HudItemAccess.canShowMoonPhase(player)) return null
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return unavailableMoonPhaseLine
        val forecastCurrent = MoonPhaseClientState.forecastDay == level.dayIndex
        val eventId = if (forecastCurrent) MoonPhaseClientState.forecastId else ""
        val event = MoonPhaseEvents.get(eventId)
        if (event != null) {
            return specialMoonPhaseLines.getOrPut(eventId) {
                val name = event.effect().displayName
                environmentLine('\uE105', Component.translatable("hud.${FTMod.MODID}.moon_phase", name))
            }
        }
        return moonPhaseLines[level.moonPhase]
    }

    fun weatherLine(level: Level, pos: BlockPos): Component = Component.empty()
        .append(environmentLine('\uE104', Component.translatable(
            "hud.${FTMod.MODID}.area_weather",
            Component.translatable(if (AreaWeatherEvents.supported(level)) {
                AreaWeatherClientState.weather?.translationKey ?: "hud.${FTMod.MODID}.weather.pending"
            } else "hud.${FTMod.MODID}.weather.unavailable")
        )))
        .append("  ")
        .append(Component.translatable(
            "hud.${FTMod.MODID}.current_weather", currentWeather(level, pos)
        ))

    private fun currentWeather(level: Level, pos: BlockPos): Component {
        val precipitation = level.getBiome(pos).value().getPrecipitationAt(pos)
        val key = when {
            !level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling() -> "unavailable"
            !level.isRaining && DarkCumulonimbusClientState.active -> "dark_cumulonimbus"
            !level.isRaining -> "clear"
            precipitation == Biome.Precipitation.NONE -> if (level.isThundering) "overcast" else "clear"
            precipitation == Biome.Precipitation.SNOW -> "snow"
            GreenRainClientState.active -> "green_rain"
            level.isThundering -> "thunder"
            else -> {
                val intensity = if (IntegrationManager.usesTropicalSeasons(level, pos)) {
                    HeavyRainClientState.tropical
                } else HeavyRainClientState.temperate
                return Component.translatable("weather.${FTMod.MODID}.${intensity.name.lowercase(Locale.ROOT)}")
            }
        }
        return Component.translatable("hud.${FTMod.MODID}.weather.$key")
    }

    data class CalendarLines(val date: Component, val seasonalTime: Component?, val time: Component)

    data class RealTimeLines(val dateTime: Component, val zone: Component)
    private var realTimeSnapshot: RealTimeClock.Snapshot? = null
    private var cachedRealTimeLines: RealTimeLines? = null

    fun realTimeLines(): RealTimeLines {
        val snapshot = RealTimeClock.current(Minecraft.getInstance().languageManager.selected)
        if (snapshot !== realTimeSnapshot) {
            realTimeSnapshot = snapshot
            val zoneText = Component.literal(snapshot.zone)
            snapshot.seasonalTime?.let { status ->
                zoneText.append(" ").append(Component.translatable(
                    "hud.${FTMod.MODID}.real_time.${status.name.lowercase(Locale.ROOT)}"
                ))
            }
            cachedRealTimeLines = RealTimeLines(
                Component.translatable("hud.${FTMod.MODID}.real_time", snapshot.dateTime),
                zoneText.withStyle(ChatFormatting.WHITE)
            )
        }
        return cachedRealTimeLines!!
    }

    fun calendarLines(level: Level): CalendarLines {
        val displayDayTime = SeasonalDaylight.displayDayTime(level)
        val day = Math.floorDiv(displayDayTime, 24000L) + 1L
        val calendar = IntegrationManager.calendarAt(level, 0L)
        val ticksPerYear = (calendar?.dayDuration?.toLong() ?: 24000L) *
            (calendar?.daysInMonth?.toLong() ?: 8L) * 12L
        val year = 1L + Math.floorDiv(displayDayTime, ticksPerYear)
        val clockTicks = (Math.floorMod(displayDayTime, 24000L) + 6000L) % 24000L
        val hour = (clockTicks / 1000L).toString().padStart(2, '0')
        val minute = (clockTicks % 1000L * 60L / 1000L).toString().padStart(2, '0')
        return CalendarLines(
            Component.translatable("hud.${FTMod.MODID}.year_and_day", year, day),
            if (SeasonalDaylight.enabledFor(level)) {
                Component.translatable("hud.${FTMod.MODID}.real_time.daylight")
            } else null,
            Component.literal("$hour:$minute")
        )
    }
}

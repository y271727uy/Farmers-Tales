package com.y271727uy.farmerstales.gameplay.clock.client

import com.ibm.icu.text.TimeZoneNames
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/** Local-only wall clock. Poll the system zone once a second; format only when the minute changes. */
object RealTimeClock {
    enum class SeasonalTime { DAYLIGHT, STANDARD }
    data class Snapshot(val dateTime: String, val zone: String, val seasonalTime: SeasonalTime?)

    private val dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
    private var checkedSecond = Long.MIN_VALUE
    private var formattedMinute = Long.MIN_VALUE
    private var language = ""
    private var locale = Locale.ROOT
    private var zone: ZoneId? = null
    private var snapshot: Snapshot? = null

    fun current(languageCode: String): Snapshot {
        val second = Math.floorDiv(System.currentTimeMillis(), 1000L)
        val languageChanged = language != languageCode
        if (second == checkedSecond && !languageChanged) snapshot?.let { return it }
        checkedSecond = second
        if (languageChanged) {
            language = languageCode
            locale = Locale.forLanguageTag(languageCode.replace('_', '-'))
        }
        val currentZone = ZoneId.systemDefault()
        val minute = Math.floorDiv(second, 60L)
        if (snapshot == null || minute != formattedMinute || zone != currentZone || languageChanged) {
            snapshot = format(Instant.ofEpochSecond(second), currentZone, locale)
            formattedMinute = minute
            zone = currentZone
        }
        return snapshot!!
    }

    fun format(instant: Instant, zone: ZoneId, locale: Locale): Snapshot {
        val local = instant.atZone(zone)
        val offsetSeconds = local.offset.totalSeconds
        val magnitude = abs(offsetSeconds)
        val hours = magnitude / 3600
        val minutes = magnitude / 60 % 60
        val seconds = magnitude % 60
        val offset = buildString {
            append("UTC").append(if (offsetSeconds < 0) '-' else '+').append(hours)
            if (minutes != 0 || seconds != 0) append(':').append(minutes.toString().padStart(2, '0'))
            if (seconds != 0) append(':').append(seconds.toString().padStart(2, '0'))
        }
        // A zone's exemplar city describes the configured time zone, not the player's location.
        // Fixed offsets and Etc/* zones have no city; never invent one from their offset.
        val city = if ('/' in zone.id && !zone.id.startsWith("Etc/")) {
            TimeZoneNames.getInstance(locale).getExemplarLocationName(zone.id)
        } else null
        return Snapshot(dateTimeFormat.format(local), if (city.isNullOrBlank()) offset else "$offset $city",
            seasonalTime(instant, zone))
    }

    private fun seasonalTime(instant: Instant, zone: ZoneId): SeasonalTime? {
        val rules = zone.rules
        if (rules.isFixedOffset) return null
        val currentSaving = rules.getDaylightSavings(instant).seconds
        val horizon = instant.plus(Duration.ofDays(370))
        var transition = rules.nextTransition(instant.minusNanos(1))
        // Only inspect upcoming rules, not historical DST (e.g. Shanghai before 1992).
        // Bounded and called only while formatting a new minute, never every frame.
        repeat(8) {
            val next = transition ?: return if (currentSaving == 0L) null else SeasonalTime.DAYLIGHT
            if (next.instant.isAfter(horizon)) return if (currentSaving == 0L) null else SeasonalTime.DAYLIGHT
            val before = rules.getDaylightSavings(next.instant.minusNanos(1)).seconds
            val after = rules.getDaylightSavings(next.instant).seconds
            if (before != after) {
                // Some zones (such as Dublin) encode winter with a negative DST adjustment.
                // The higher seasonal clock adjustment is summer, even when it is zero.
                return if (currentSaving == maxOf(before, after)) SeasonalTime.DAYLIGHT else SeasonalTime.STANDARD
            }
            transition = rules.nextTransition(next.instant)
        }
        return null
    }
}

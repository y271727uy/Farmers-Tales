package com.y271727uy.farmerstales.gameplay.clock.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

/** Standalone checks using the existing Kotlin stdlib and Minecraft's ICU library. */
public final class RealTimeClockTest {
    public static void main(String[] args) {
        var winter = format("2026-11-02T00:08:00Z", ZoneId.of("Europe/London"), Locale.SIMPLIFIED_CHINESE);
        check(winter.getDateTime().equals("02/11/2026 00:08"), "day/month/year format");
        check(winter.getZone().equals("UTC+0 伦敦"), "localized London and winter offset: " + winter.getZone());
        check(winter.getSeasonalTime() == RealTimeClock.SeasonalTime.STANDARD, "London winter label");
        var summer = format("2024-07-01T12:00:00Z", ZoneId.of("Europe/London"), Locale.ENGLISH);
        check(summer.getDateTime().equals("01/07/2024 13:00"), "summer wall clock");
        check(summer.getZone().equals("UTC+1 London"), "summer offset and English city");
        check(summer.getSeasonalTime() == RealTimeClock.SeasonalTime.DAYLIGHT, "London summer label");
        var fixed = format("2026-11-02T00:08:00Z", ZoneOffset.UTC, Locale.SIMPLIFIED_CHINESE);
        check(fixed.getZone().equals("UTC+0"), "fixed UTC must not invent a city");
        check(fixed.getSeasonalTime() == null, "fixed UTC must not get a winter label");
        var fractional = format("2026-11-02T00:08:00Z", ZoneOffset.of("+05:45"), Locale.ENGLISH);
        check(fractional.getDateTime().equals("02/11/2026 05:53"), "quarter-hour wall clock");
        check(fractional.getZone().equals("UTC+5:45"), "quarter-hour offset");
        var negative = format("2026-11-02T00:08:00Z", ZoneOffset.of("-03:30"), Locale.ENGLISH);
        check(negative.getDateTime().equals("01/11/2026 20:38"), "previous-day rollover");
        check(negative.getZone().equals("UTC-3:30"), "negative half-hour offset");
        var positive = format("2026-11-02T12:08:00Z", ZoneOffset.ofHours(14), Locale.ENGLISH);
        check(positive.getDateTime().equals("03/11/2026 02:08"), "next-day rollover");
        check(positive.getZone().equals("UTC+14"), "UTC+14");
        check(format("2024-02-29T12:00:00Z", ZoneOffset.UTC, Locale.ENGLISH).getDateTime()
                .equals("29/02/2024 12:00"), "leap day");
        check(format("2026-11-02T00:08:00Z", ZoneId.of("Etc/GMT+8"), Locale.ENGLISH).getZone()
                .equals("UTC-8"), "Etc offset must not invent a city");
        check(format("2026-11-02T00:08:00Z", ZoneId.of("Asia/Shanghai"), Locale.SIMPLIFIED_CHINESE)
                .getSeasonalTime() == null, "historical Chinese DST must not create a present label");
        check(format("2024-07-01T12:00:00Z", ZoneId.of("Asia/Kolkata"), Locale.ENGLISH)
                .getSeasonalTime() == null, "non-DST region must not get a label");
        check(format("2024-01-15T12:00:00Z", ZoneId.of("Australia/Sydney"), Locale.ENGLISH)
                .getSeasonalTime() == RealTimeClock.SeasonalTime.DAYLIGHT, "southern hemisphere summer");
        check(format("2024-07-15T12:00:00Z", ZoneId.of("Australia/Sydney"), Locale.ENGLISH)
                .getSeasonalTime() == RealTimeClock.SeasonalTime.STANDARD, "southern hemisphere winter");
        check(format("2024-01-15T12:00:00Z", ZoneId.of("Europe/Dublin"), Locale.ENGLISH)
                .getSeasonalTime() == RealTimeClock.SeasonalTime.STANDARD, "negative DST winter convention");
        check(format("2024-07-15T12:00:00Z", ZoneId.of("Europe/Dublin"), Locale.ENGLISH)
                .getSeasonalTime() == RealTimeClock.SeasonalTime.DAYLIGHT, "negative DST summer convention");
        System.out.println("PASS: clock formatting, localization, DST labels, non-DST and historical-only regions, southern hemisphere and negative DST conventions");
    }

    private static RealTimeClock.Snapshot format(String instant, ZoneId zone, Locale locale) {
        return RealTimeClock.INSTANCE.format(Instant.parse(instant), zone, locale);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

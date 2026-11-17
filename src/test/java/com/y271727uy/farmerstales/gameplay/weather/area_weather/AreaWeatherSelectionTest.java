package com.y271727uy.farmerstales.gameplay.weather.area_weather;

import java.util.EnumSet;
import java.util.Random;

/** Standalone regression checks; requires only compiled AreaWeather and the existing Kotlin stdlib. */
public final class AreaWeatherSelectionTest {
    public static void main(String[] args) {
        var winterOnly = EnumSet.of(AreaWeather.LIGHT_SNOW, AreaWeather.HEAVY_SNOW, AreaWeather.HAIL);
        var summerOnly = EnumSet.of(AreaWeather.THUNDERSHOWERS, AreaWeather.HEATWAVE);
        check(AreaWeather.values().length == 15, "candidate count");
        var random = new Random(271727);
        int radiation = 0;
        int rolls = 0;
        for (int monthIndex = -1; monthIndex < 12; monthIndex++) {
            Integer month = monthIndex < 0 ? null : monthIndex;
            var seen = EnumSet.noneOf(AreaWeather.class);
            for (int i = 0; i < 100_000; i++) {
                var weather = AreaWeather.Companion.select(month, random.nextDouble(), random::nextInt);
                seen.add(weather);
                if (winterOnly.contains(weather)) check(month != null && month >= 9, "winter-only weather outside winter");
                if (summerOnly.contains(weather)) check(month != null && month >= 3 && month <= 5, "summer-only weather outside summer");
                if (weather == AreaWeather.FROST) check(Integer.valueOf(8).equals(month), "frost outside late autumn");
                if (weather == AreaWeather.NUCLEAR_RADIATION) radiation++;
                rolls++;
            }
            for (var weather : AreaWeather.values()) {
                boolean expected = winterOnly.contains(weather) ? month != null && month >= 9
                        : summerOnly.contains(weather) ? month != null && month >= 3 && month <= 5
                        : weather == AreaWeather.FROST ? Integer.valueOf(8).equals(month) : true;
                check(seen.contains(weather) == expected, "missing or forbidden candidate: " + weather + " month=" + month);
            }
            check(AreaWeather.Companion.select(month, 0.009999, bound -> { throw new AssertionError("radiation used ordinary pool"); })
                    == AreaWeather.NUCLEAR_RADIATION, "radiation threshold below 1%");
            check(AreaWeather.Companion.select(month, 0.01, bound -> 0) != AreaWeather.NUCLEAR_RADIATION, "radiation threshold at 1%");
        }
        double rate = (double) radiation / rolls;
        check(Math.abs(rate - 0.01) < 0.0005, "radiation rate=" + rate);
        check(AreaWeather.Companion.byId("invalid") == null, "unknown saved weather");
        check(AreaWeather.Companion.byId("BREEZE") == AreaWeather.BREEZE, "weather id round-trip");
        System.out.printf("PASS: %,d rolls; all seasonal restrictions and candidates checked; radiation %.4f%%%n", rolls, rate * 100);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

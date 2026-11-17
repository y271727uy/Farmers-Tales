package com.y271727uy.farmerstales.backport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads {@code config/farmerstales/farmerstales-backports.toml}.
 *
 * <p>Registration happens during mod construction, before Forge loads COMMON configs, and the
 * mixin plugin needs the values even earlier. This file is therefore read directly on first use
 * instead of going through {@code ForgeConfigSpec}. Changes require a restart, and the client must
 * use the same values as the server it joins.
 */
public final class BackportConfig {
    public static final String FILE_NAME = com.y271727uy.farmerstales.config.ConfigFiles.BACKPORTS;
    /** {@code -Dfarmerstales.backports.all=true} forces every feature on (used by data generation). */
    private static final String FORCE_ALL_PROPERTY = "farmerstales.backports.all";

    private static volatile Map<BackportFeature, Boolean> values;

    private BackportConfig() {}

    public static boolean isEnabled(BackportFeature feature) {
        return load().getOrDefault(feature, false);
    }

    /** By name, for callers that must not load {@link BackportFeature} through other means. */
    public static boolean isEnabled(String featureId) {
        var feature = BackportFeature.byId(featureId);
        return feature == null || isEnabled(feature);
    }

    private static Map<BackportFeature, Boolean> load() {
        var loaded = values;
        if (loaded != null) return loaded;
        synchronized (BackportConfig.class) {
            if (values == null) values = read();
            return values;
        }
    }

    private static Map<BackportFeature, Boolean> read() {
        Map<BackportFeature, Boolean> result = new EnumMap<>(BackportFeature.class);
        // Back-ported content is opt-in: every feature is off unless the file turns it on.
        boolean all = forceAll();
        for (BackportFeature feature : BackportFeature.values()) result.put(feature, all);
        if (all) return result;

        com.y271727uy.farmerstales.config.ConfigFiles.migrate("farmerstales-backports.toml", FILE_NAME);
        Path file = com.y271727uy.farmerstales.config.ConfigFiles.configDir().resolve(FILE_NAME);
        boolean complete = false;
        if (Files.isRegularFile(file)) {
            try {
                int found = 0;
                for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String line = raw.strip();
                    int comment = line.indexOf('#');
                    if (comment >= 0) line = line.substring(0, comment).strip();
                    int equals = line.indexOf('=');
                    if (line.isEmpty() || line.startsWith("[") || equals < 0) continue;
                    var feature = BackportFeature.byId(line.substring(0, equals).strip().replace("\"", ""));
                    String value = line.substring(equals + 1).strip().toLowerCase(Locale.ROOT);
                    if (feature == null || !(value.equals("true") || value.equals("false"))) continue;
                    result.put(feature, Boolean.parseBoolean(value));
                    found++;
                }
                complete = found >= BackportFeature.values().length;
            } catch (IOException exception) {
                System.err.println("[farmerstales] Could not read " + file + ", enabling all backports: " + exception);
                return result;
            }
        }
        if (!complete) write(file, result);
        return Map.copyOf(result);
    }

    private static void write(Path file, Map<BackportFeature, Boolean> values) {
        var text = new StringBuilder()
                .append("# Farmer's Tales: content back-ported from newer Minecraft versions.\n")
                .append("# Every feature is OFF by default. Set an entry to true to register its blocks, items, entities,\n")
                .append("# biomes, sounds and data.\n")
                .append("# - Requires a game restart.\n")
                .append("# - Clients and the server they join must use the same values.\n")
                .append("#\n")
                .append("# WARNING: back up your world before changing these values.\n")
                .append("# - Turning a feature OFF in an existing world permanently deletes its blocks, items and entities\n")
                .append("#   from that world (placed blocks become air, items vanish from inventories and chests).\n")
                .append("# - Turning a feature ON in an existing world does not change chunks that were already\n")
                .append("#   generated; its biomes and natural blocks only appear in new chunks.\n");
        for (BackportFeature feature : BackportFeature.values()) {
            text.append("\n# [").append(feature.version()).append("] ").append(feature.description()).append('\n')
                    .append(feature.id()).append(" = ").append(values.get(feature)).append('\n');
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            System.err.println("[farmerstales] Could not write " + file + ": " + exception);
        }
    }

    private static boolean forceAll() {
        if (Boolean.getBoolean(FORCE_ALL_PROPERTY)) return true;
        try {
            return net.minecraftforge.fml.loading.FMLLoader.getLaunchHandler().isData();
        } catch (Throwable ignored) {
            return false;
        }
    }


    /** Feature ids enabled right now, in declaration order. */
    public static List<BackportFeature> enabled() {
        return java.util.Arrays.stream(BackportFeature.values()).filter(BackportConfig::isEnabled).toList();
    }
}

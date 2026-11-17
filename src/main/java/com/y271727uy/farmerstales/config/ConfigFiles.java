package com.y271727uy.farmerstales.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Locations of Farmer's Tales config files, all kept under {@code config/farmerstales/}. */
public final class ConfigFiles {
    public static final String DIR = "farmerstales";
    /** Gameplay settings ({@code ForgeConfigSpec}, registered as COMMON). */
    public static final String GAMEPLAY = DIR + "/farmerstales-gameplay.toml";
    /** Back-port switches, read directly by {@code BackportConfig}. */
    public static final String BACKPORTS = DIR + "/farmerstales-backports.toml";

    private ConfigFiles() {}

    public static Path configDir() {
        try {
            return net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
        } catch (Throwable ignored) {
            return Path.of("config");
        }
    }

    /** Moves {@code config/<oldName>} to {@code config/<newName>} once, keeping settings from older versions. */
    public static void migrate(String oldName, String newName) {
        Path oldFile = configDir().resolve(oldName);
        Path newFile = configDir().resolve(newName);
        if (!Files.isRegularFile(oldFile) || Files.exists(newFile)) return;
        try {
            Files.createDirectories(newFile.getParent());
            Files.move(oldFile, newFile);
        } catch (IOException exception) {
            System.err.println("[farmerstales] Could not move " + oldFile + " to " + newFile + ": " + exception);
        }
    }
}

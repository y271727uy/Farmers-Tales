package com.y271727uy.farmerstales.backport;

import java.util.Locale;

/**
 * Groups of content back-ported from newer Minecraft versions. Each constant is one switch in
 * {@code config/farmerstales/farmerstales-backports.toml}.
 *
 * <p>This class must stay free of Minecraft references: the mixin plugin reads it during launch.
 */
public enum BackportFeature {
    ARMORED_PAWS("1.20.5", "Armadillo, armadillo scute and wolf armor."),
    WOLF_VARIANTS("1.20.5", "Biome-based wolf coat variants (pale, ashen, black, chestnut, rusty, snowy, spotted, striped, woods)."),
    TUFF_VARIANTS("1.21", "Polished tuff, tuff bricks and their stairs, slabs, walls and chiseled blocks."),
    PALE_GARDEN("1.21.4", "Pale Garden biome, pale oak wood set, pale moss, eyeblossoms, creaking, creaking heart and resin blocks."),
    SPRING_PLANTS("1.21.5", "Leaf litter, bush, firefly bush, wildflowers, cactus flower, dry grass, fallen trees and desert ambience."),
    FARM_ANIMAL_VARIANTS("1.21.5", "Cold/warm cow, pig and chicken variants, plus blue and brown eggs."),
    WOLF_SOUND_VARIANTS("1.21.5", "Angry, big, cute, grumpy, puglin and sad wolf sound sets."),
    SHELF("1.21.9", "Wooden shelves."),
    NAUTILUS("1.21.11", "Nautilus and zombie nautilus."),
    GOLDEN_DANDELION("26.1", "Golden dandelion, which pauses or resumes the growth of baby animals."),
    POPLAR("26.3", "Dappled Forest biome, poplar wood set (incl. signs, boats, shelf), red shrub and shelf mushroom.");

    private final String version;
    private final String description;

    BackportFeature(String version, String description) {
        this.version = version;
        this.description = description;
    }

    /** Config key, e.g. {@code pale_garden}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The Minecraft version that introduced this content. */
    public String version() {
        return version;
    }

    public String description() {
        return description;
    }

    public boolean isEnabled() {
        return BackportConfig.isEnabled(this);
    }

    public static BackportFeature byId(String id) {
        for (BackportFeature feature : values()) {
            if (feature.id().equals(id) || feature.name().equals(id)) return feature;
        }
        return null;
    }
}

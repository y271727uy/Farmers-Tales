package com.y271727uy.farmerstales.backport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks content back-ported from a newer Minecraft version. Whether it exists at runtime is
 * controlled by {@code config/farmerstales/farmerstales-backports.toml}.
 *
 * <ul>
 *     <li><b>Content class</b>: if it declares {@code public static void register(IEventBus)},
 *     {@link BackportLoader} calls it only when the feature is enabled. Do not call it from
 *     {@code FTMod} as well.</li>
 *     <li><b>Event handler class</b>: use this in place of {@code @Mod.EventBusSubscriber} and
 *     list the buses in {@link #subscribe()}; the class is subscribed only when enabled.</li>
 *     <li><b>Mixin class</b>: the mixin is skipped entirely when the feature is disabled.</li>
 *     <li><b>{@code RegisteredMob} field</b> in a {@code MobRegistry}: the mob, its spawn egg,
 *     attributes, spawn placement and natural spawns are registered only when enabled.</li>
 * </ul>
 *
 * Data (recipes, loot tables, tags, worldgen) for a feature lives in the built-in data pack
 * {@code backports/<feature id>/}, which is only loaded when the feature is enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Backport {
    BackportFeature value();

    /** Event buses this class's static {@code @SubscribeEvent} methods are registered to. */
    Bus[] subscribe() default {};

    /** Only load/subscribe this class on the physical client. */
    boolean clientOnly() default false;

    enum Bus { FORGE, MOD }
}

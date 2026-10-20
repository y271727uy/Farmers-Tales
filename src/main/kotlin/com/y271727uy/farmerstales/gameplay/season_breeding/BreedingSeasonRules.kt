package com.y271727uy.farmerstales.gameplay.season_breeding

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType

/** Resolves breeding seasons from entity type tags so datapacks and other mods can extend them. */
object BreedingSeasonRules {
    private val seasonTags = linkedMapOf(
        SeasonWindow.SPRING to entityTypeTag("spring_breeders"),
        SeasonWindow.SUMMER to entityTypeTag("summer_breeders"),
        SeasonWindow.AUTUMN to entityTypeTag("autumn_breeders"),
        SeasonWindow.WINTER to entityTypeTag("winter_breeders"),
    )
    private val yearRoundTag = entityTypeTag("year_round_breeders")

    fun hasRule(entityType: EntityType<*>): Boolean =
        entityType.builtInRegistryHolder().`is`(yearRoundTag) || seasonTags.values.any { tag ->
            entityType.builtInRegistryHolder().`is`(tag)
        }

    fun allowedSeasons(entityType: EntityType<*>): Collection<SeasonWindow> {
        if (entityType.builtInRegistryHolder().`is`(yearRoundTag)) {
            return SeasonWindow.entries.toSet()
        }

        return seasonTags
            .filter { (_, tag) -> entityType.builtInRegistryHolder().`is`(tag) }
            .keys
            .toSet()
    }

    private fun entityTypeTag(path: String): TagKey<EntityType<*>> = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(FTMod.MODID, path),
    )
}

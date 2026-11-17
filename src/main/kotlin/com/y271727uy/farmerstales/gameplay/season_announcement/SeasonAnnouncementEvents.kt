package com.y271727uy.farmerstales.gameplay.season_announcement

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.announce
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import glitchcore.event.EventManager
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.server.level.ServerLevel
import sereneseasons.api.season.Season
import sereneseasons.api.season.SeasonChangedEvent
import sereneseasons.api.season.SeasonHelper

/** Announces a season's arrival only to players in the climate zone it applies to. */
object SeasonAnnouncementEvents {
    fun register() {
        EventManager.addListener<SeasonChangedEvent.Standard>(::onSeasonChanged)
        EventManager.addListener<SeasonChangedEvent.Tropical>(::onTropicalSeasonChanged)
    }

    private fun onSeasonChanged(event: SeasonChangedEvent.Standard) {
        if (event.level.isClientSide || !IntegrationManager.isSereneSeasonsLoaded()) {
            return
        }

        val season = event.newSeason.toSeasonWindow() ?: return
        val message = Component.translatable(
            "message.${FTMod.MODID}.season_started.${season.name.lowercase()}",
        ).withStyle(season.chatFormatting())
        sendToPlayersInClimate(event.level, usesTropicalSeasons = false, message)
    }

    private fun onTropicalSeasonChanged(event: SeasonChangedEvent.Tropical) {
        if (event.level.isClientSide || !IntegrationManager.isSereneSeasonsLoaded()) {
            return
        }

        val tropicalSeason = event.newSeason.takeIf {
            it == Season.TropicalSeason.EARLY_DRY || it == Season.TropicalSeason.EARLY_WET
        } ?: return
        val climate = if (tropicalSeason == Season.TropicalSeason.EARLY_DRY) "dry" else "wet"
        val message = Component.translatable("message.${FTMod.MODID}.season_started.$climate")
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(tropicalSeason.foliageOverlay)))
        sendToPlayersInClimate(event.level, usesTropicalSeasons = true, message)
    }

    private fun Season.SubSeason.toSeasonWindow(): SeasonWindow? = when (this) {
        Season.SubSeason.EARLY_SPRING -> SeasonWindow.SPRING
        Season.SubSeason.EARLY_SUMMER -> SeasonWindow.SUMMER
        Season.SubSeason.EARLY_AUTUMN -> SeasonWindow.AUTUMN
        Season.SubSeason.EARLY_WINTER -> SeasonWindow.WINTER
        else -> null
    }

    private fun sendToPlayersInClimate(level: net.minecraft.world.level.Level, usesTropicalSeasons: Boolean, message: Component) {
        (level as? ServerLevel)?.announce(message) { player ->
            SeasonHelper.usesTropicalSeasons(level.getBiome(player.blockPosition())) == usesTropicalSeasons
        }
    }
}

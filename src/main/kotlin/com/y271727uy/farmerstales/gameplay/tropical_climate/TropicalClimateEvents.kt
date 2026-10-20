package com.y271727uy.farmerstales.gameplay.tropical_climate

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.integration.IntegrationManager
import glitchcore.event.EventManager
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import sereneseasons.api.season.Season
import sereneseasons.api.season.SeasonChangedEvent
import sereneseasons.api.season.SeasonHelper

/**
 * Selects one exceptional climate pattern at each tropical season transition.
 * The pattern is intentionally independent from Minecraft's dimension-wide
 * weather and is available for Farmers' Tales mechanics to consult later.
 */
object TropicalClimateEvents {
    fun register() {
        EventManager.addListener<SeasonChangedEvent.Tropical>(::onTropicalSeasonChanged)
    }

    /** Current exceptional pattern for this dimension; NONE means a normal season. */
    fun currentPattern(level: ServerLevel): TropicalClimatePattern = TropicalClimateData.get(level).pattern

    fun isPatternActiveAt(level: Level, pos: BlockPos, pattern: TropicalClimatePattern): Boolean {
        val serverLevel = level as? ServerLevel ?: return false
        return currentPattern(serverLevel) == pattern &&
            SeasonHelper.usesTropicalSeasons(serverLevel.getBiome(pos))
    }

    private fun onTropicalSeasonChanged(event: SeasonChangedEvent.Tropical) {
        val level = event.level as? ServerLevel ?: return
        if (!Config.tropicalClimateEventsEnabled || !IntegrationManager.isSereneSeasonsLoaded()) {
            return
        }

        val pattern = when (event.newSeason) {
            Season.TropicalSeason.EARLY_WET -> TropicalClimatePattern.MONSOON.takeIf {
                level.random.nextDouble() < Config.tropicalMonsoonChance
            }
            Season.TropicalSeason.EARLY_DRY -> TropicalClimatePattern.DROUGHT.takeIf {
                level.random.nextDouble() < Config.tropicalDroughtChance
            }
            else -> return
        } ?: TropicalClimatePattern.NONE

        TropicalClimateData.get(level).setPattern(pattern)
        if (pattern != TropicalClimatePattern.NONE) {
            sendToTropicalPlayers(level, Component.translatable("message.${FTMod.MODID}.tropical_climate.${pattern.name.lowercase()}"))
        }
    }

    private fun sendToTropicalPlayers(level: ServerLevel, message: Component) {
        level.players()
            .filterIsInstance<ServerPlayer>()
            .filter { player -> SeasonHelper.usesTropicalSeasons(level.getBiome(player.blockPosition())) }
            .forEach { it.sendSystemMessage(message) }
    }
}

enum class TropicalClimatePattern {
    NONE,
    MONSOON,
    DROUGHT,
}

private class TropicalClimateData : SavedData() {
    var pattern: TropicalClimatePattern = TropicalClimatePattern.NONE
        private set
    fun setPattern(value: TropicalClimatePattern) {
        pattern = value
        setDirty()
    }

    override fun save(tag: CompoundTag): CompoundTag = tag.apply {
        putString("Pattern", pattern.name)
    }

    companion object {
        private const val DATA_NAME = "farmerstales_tropical_climate"

        fun get(level: ServerLevel): TropicalClimateData =
            level.dataStorage.computeIfAbsent(::load, ::TropicalClimateData, DATA_NAME)

        private fun load(tag: CompoundTag): TropicalClimateData = TropicalClimateData().apply {
            pattern = runCatching { TropicalClimatePattern.valueOf(tag.getString("Pattern")) }
                .getOrDefault(TropicalClimatePattern.NONE)
        }
    }
}

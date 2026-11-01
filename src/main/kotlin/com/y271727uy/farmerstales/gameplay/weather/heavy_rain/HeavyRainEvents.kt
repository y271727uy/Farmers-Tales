package com.y271727uy.farmerstales.gameplay.weather.heavy_rain

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainEvents
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import com.y271727uy.farmerstales.network.RainIntensitySyncPacket
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier

/** One roll and two calendar-specific results per dimension's storm, with no region tracking. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object HeavyRainEvents {
    fun intensityAt(level: ServerLevel, pos: BlockPos): RainIntensity {
        if (!Config.heavyRainEnabled || !level.isRaining || GreenRainEvents.isActive(level)) return RainIntensity.NORMAL
        val state = update(level)
        return if (IntegrationManager.usesTropicalSeasons(level, pos)) state.tropical else state.temperate
    }

    /** Debug override for this storm, applied equally to both climate calendars. */
    fun force(level: ServerLevel, intensity: RainIntensity): Boolean {
        if (!level.isRaining) return false
        GreenRainEvents.clearForStorm(level)
        val state = data(level)
        state.wasRaining = true
        state.temperate = intensity
        state.tropical = intensity
        state.setDirty()
        syncAll(level, state)
        return true
    }

    @JvmStatic
    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        val level = event.level as? ServerLevel ?: return
        if (event.phase == TickEvent.Phase.END && level.dimensionType().hasSkyLight()) update(level)
    }

    private fun update(level: ServerLevel): RainData {
        val state = data(level)
        if (state.wasRaining == level.isRaining) return state
        state.wasRaining = level.isRaining
        state.temperate = RainIntensity.NORMAL
        state.tropical = RainIntensity.NORMAL
        if (state.wasRaining && Config.heavyRainEnabled && level.dimensionType().hasSkyLight()) {
            val roll = level.random.nextDouble()
            val (summer, wet) = IntegrationManager.rainSeasonBoosts(level)
            fun select(boost: Boolean) = RainIntensity.select(
                roll, Config.heavyRainChance, Config.torrentialRainChance,
                if (boost) Config.heavyRainSeasonMultiplier else 1.0,
            )
            state.temperate = select(summer)
            state.tropical = select(wet)
        }
        state.setDirty()
        syncAll(level, state)
        return state
    }

    private fun syncAll(level: ServerLevel, state: RainData) {
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.DIMENSION.with(Supplier { level.dimension() }),
            RainIntensitySyncPacket(state.temperate, state.tropical),
        )
    }

    private fun syncPlayer(player: ServerPlayer) {
        val state = update(player.serverLevel())
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(Supplier { player }),
            RainIntensitySyncPacket(state.temperate, state.tropical),
        )
    }

    @JvmStatic
    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        (event.entity as? ServerPlayer)?.let(::syncPlayer)
    }

    @JvmStatic
    @SubscribeEvent
    fun onDimensionChange(event: PlayerEvent.PlayerChangedDimensionEvent) {
        (event.entity as? ServerPlayer)?.let(::syncPlayer)
    }

    @JvmStatic
    @SubscribeEvent
    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        (event.entity as? ServerPlayer)?.let(::syncPlayer)
    }

    private fun data(level: ServerLevel): RainData =
        level.dataStorage.computeIfAbsent(::load, ::RainData, "${FTMod.MODID}_rain_intensity")

    private fun load(tag: CompoundTag) = RainData().apply {
        wasRaining = tag.getBoolean("WasRaining")
        temperate = RainIntensity.entries.getOrNull(tag.getInt("Temperate")) ?: RainIntensity.NORMAL
        tropical = RainIntensity.entries.getOrNull(tag.getInt("Tropical")) ?: RainIntensity.NORMAL
    }

    private class RainData : SavedData() {
        var wasRaining = false
        var temperate = RainIntensity.NORMAL
        var tropical = RainIntensity.NORMAL

        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putBoolean("WasRaining", wasRaining)
            putInt("Temperate", temperate.ordinal)
            putInt("Tropical", tropical.ordinal)
        }
    }
}

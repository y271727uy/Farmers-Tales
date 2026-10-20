package com.y271727uy.farmerstales.gameplay.weather.green_rain

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import com.y271727uy.farmerstales.network.GreenRainSyncPacket
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier

/**
 * Dimension-wide fork of vanilla rain. Each summer storm rolls once on the
 * rain rising edge; there is no pity timer. The flag lasts until rain stops.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object GreenRainEvents {
    private const val DATA_NAME = "${FTMod.MODID}_green_rain"
    private const val ACTIVE_TAG = "Active"
    private const val WAS_RAINING_TAG = "WasRaining"

    fun isActive(level: ServerLevel): Boolean = data(level).active

    /**
     * Debug override for the current storm. Returns false if this dimension
     * is not raining, because rain-stop would clear the flag on the next tick.
     */
    fun force(level: ServerLevel): Boolean {
        if (!level.isRaining) return false
        val state = data(level)
        state.wasRaining = true
        if (!state.active) {
            state.active = true
            syncAll(level)
        }
        state.setDirty()
        return true
    }

    @JvmStatic
    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val level = event.level as? ServerLevel ?: return
        if (!level.dimensionType().hasSkyLight()) return

        val state = data(level)
        val raining = level.isRaining
        if (!raining) {
            if (state.active || state.wasRaining) {
                state.active = false
                state.wasRaining = false
                state.setDirty()
                syncAll(level)
            }
            return
        }

        if (state.active) {
            if (!state.wasRaining) {
                state.wasRaining = true
                state.setDirty()
            }
            return
        }

        if (state.wasRaining) return

        state.wasRaining = true
        if (canRoll(level) && level.random.nextDouble() < Config.greenRainChance) {
            state.active = true
            level.players().filterIsInstance<ServerPlayer>().forEach { player ->
                player.sendSystemMessage(Component.translatable("message.${FTMod.MODID}.green_rain.started"))
            }
            syncAll(level)
        }
        state.setDirty()
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        val player = event.entity as? ServerPlayer ?: return
        syncPlayer(player)
    }

    private fun canRoll(level: ServerLevel): Boolean {
        if (!Config.greenRainEnabled) return false
        return IntegrationManager.currentSeasonName(level) == "summer"
    }

    private fun syncAll(level: ServerLevel) {
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.DIMENSION.with(Supplier { level.dimension() }),
            GreenRainSyncPacket(data(level).active),
        )
    }

    private fun syncPlayer(player: ServerPlayer) {
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(Supplier { player }),
            GreenRainSyncPacket(data(player.serverLevel()).active),
        )
    }

    private fun data(level: ServerLevel): GreenRainData =
        level.dataStorage.computeIfAbsent(::load, ::GreenRainData, DATA_NAME)

    private fun load(tag: CompoundTag): GreenRainData = GreenRainData().apply {
        active = tag.getBoolean(ACTIVE_TAG)
        wasRaining = tag.getBoolean(WAS_RAINING_TAG)
    }

    private class GreenRainData : SavedData() {
        var active = false
        var wasRaining = false

        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putBoolean(ACTIVE_TAG, active)
            putBoolean(WAS_RAINING_TAG, wasRaining)
        }
    }
}

package com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.network.DarkCumulonimbusSyncPacket
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.world.level.saveddata.SavedData
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.network.PacketDistributor
import java.util.function.Supplier

/**
 * Dimension-wide dry overcast. It never sets vanilla raining, so rain-gated
 * systems stay clear. Real rain ends it on the same tick.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object DarkCumulonimbusEvents {
    private const val DATA_NAME = "${FTMod.MODID}_dark_cumulonimbus"
    private const val ACTIVE_TAG = "Active"
    private const val REMAINING_TAG = "RemainingTicks"
    private const val ROLLED_DAY_TAG = "RolledDay"

    fun isActive(level: ServerLevel): Boolean = data(level).active

    /** Debug override. Fails while vanilla rain owns the sky. */
    fun force(level: ServerLevel): Boolean {
        if (level.isRaining) return false
        val state = data(level)
        state.active = true
        state.remainingTicks = Config.darkCumulonimbusDurationTicks
        state.setDirty()
        announce(level)
        syncAll(level)
        return true
    }

    fun clear(level: ServerLevel): Boolean {
        val state = data(level)
        if (!state.active && state.remainingTicks == 0) return false
        end(level, state)
        return true
    }

    @JvmStatic
    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        val level = event.level as? ServerLevel ?: return
        if (!level.dimensionType().hasSkyLight()) return
        if (level.gameTime % 20L != 0L) return

        val state = data(level)
        if (level.isRaining) {
            if (state.active) end(level, state)
            return
        }

        if (state.active) {
            state.remainingTicks -= 20
            if (state.remainingTicks <= 0) {
                end(level, state)
            } else {
                state.setDirty()
                rollThunder(level)
            }
            return
        }

        val day = level.dayTime / 24_000L
        if (state.rolledDay == day) return
        state.rolledDay = day
        state.setDirty()
        if (!Config.darkCumulonimbusEnabled) return
        if (level.random.nextDouble() >= Config.darkCumulonimbusChance) return

        state.active = true
        state.remainingTicks = Config.darkCumulonimbusDurationTicks
        state.setDirty()
        announce(level)
        syncAll(level)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerLogin(event: PlayerEvent.PlayerLoggedInEvent) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerChangedDimension(event: PlayerEvent.PlayerChangedDimensionEvent) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    @JvmStatic
    @SubscribeEvent
    fun onPlayerRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    private fun rollThunder(level: ServerLevel) {
        val players = level.players()
        if (players.isEmpty()) return
        if (level.random.nextDouble() >= Config.darkCumulonimbusThunderChance) return
        val listener = players[level.random.nextInt(players.size)]
        val angle = level.random.nextDouble() * Mth.TWO_PI
        val distance = 96.0 + level.random.nextDouble() * 160.0
        val x = listener.x + Mth.cos(angle.toFloat()) * distance
        val z = listener.z + Mth.sin(angle.toFloat()) * distance
        val y = listener.y + 48.0 + level.random.nextDouble() * 32.0
        level.playSound(
            null,
            x,
            y,
            z,
            SoundEvents.LIGHTNING_BOLT_THUNDER,
            SoundSource.WEATHER,
            10000.0f,
            0.8f + level.random.nextFloat() * 0.2f,
        )
    }

    private fun end(level: ServerLevel, state: DarkCumulonimbusData) {
        state.active = false
        state.remainingTicks = 0
        state.setDirty()
        syncAll(level)
    }

    private fun announce(level: ServerLevel) {
        val message = Component.translatable("message.${FTMod.MODID}.dark_cumulonimbus.started")
        level.players().filterIsInstance<ServerPlayer>().forEach { it.sendSystemMessage(message) }
    }

    private fun syncAll(level: ServerLevel) {
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.DIMENSION.with(Supplier { level.dimension() }),
            DarkCumulonimbusSyncPacket(data(level).active),
        )
    }

    private fun syncPlayer(player: ServerPlayer) {
        FarmerstalesNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with(Supplier { player }),
            DarkCumulonimbusSyncPacket(data(player.serverLevel()).active),
        )
    }

    private fun data(level: ServerLevel): DarkCumulonimbusData =
        level.dataStorage.computeIfAbsent(::load, ::DarkCumulonimbusData, DATA_NAME)

    private fun load(tag: CompoundTag): DarkCumulonimbusData = DarkCumulonimbusData().apply {
        active = tag.getBoolean(ACTIVE_TAG)
        remainingTicks = tag.getInt(REMAINING_TAG)
        rolledDay = if (tag.contains(ROLLED_DAY_TAG)) tag.getLong(ROLLED_DAY_TAG) else -1L
    }

    private class DarkCumulonimbusData : SavedData() {
        var active = false
        var remainingTicks = 0
        var rolledDay = -1L

        override fun save(tag: CompoundTag): CompoundTag = tag.apply {
            putBoolean(ACTIVE_TAG, active)
            putInt(REMAINING_TAG, remainingTicks)
            putLong(ROLLED_DAY_TAG, rolledDay)
        }
    }
}

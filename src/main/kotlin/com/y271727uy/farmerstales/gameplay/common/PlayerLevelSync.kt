package com.y271727uy.farmerstales.gameplay.common

import com.y271727uy.farmerstales.FTMod
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.concurrent.CopyOnWriteArrayList

/**
 * One place for "this player now needs the state of the level they are in":
 * login, dimension change and respawn. Per-dimension systems register their
 * single-player sync here instead of repeating the three handlers.
 *
 * Register from the owning object's `init` block. Those objects are event-bus
 * subscribers, so they are initialised during mod construction, before any login.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object PlayerLevelSync {
    private val listeners = CopyOnWriteArrayList<(ServerPlayer) -> Unit>()

    fun register(listener: (ServerPlayer) -> Unit) {
        listeners += listener
    }

    @JvmStatic
    @SubscribeEvent
    fun onLogin(event: PlayerEvent.PlayerLoggedInEvent) = dispatch(event)

    @JvmStatic
    @SubscribeEvent
    fun onDimensionChange(event: PlayerEvent.PlayerChangedDimensionEvent) = dispatch(event)

    @JvmStatic
    @SubscribeEvent
    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) = dispatch(event)

    private fun dispatch(event: PlayerEvent) {
        val player = event.entity as? ServerPlayer ?: return
        listeners.forEach { it(player) }
    }
}

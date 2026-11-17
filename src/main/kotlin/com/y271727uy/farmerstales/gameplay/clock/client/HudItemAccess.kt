package com.y271727uy.farmerstales.gameplay.clock.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModItems
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.integration.curios.CuriosItemIntegration
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object HudItemAccess {
    private val curiosLoaded by lazy { ModList.get().isLoaded("curios") }
    private val pocketWatch = ItemPresence { ModItems.POCKET_WATCH.get() }
    private val moonPhaseDial = ItemPresence { ModItems.MOON_PHASE_DIAL.get() }

    fun canShowRealTime(player: Player): Boolean =
        !Config.realTimeRequiresPocketWatch || pocketWatch.isPresent(player)

    fun canShowMoonPhase(player: Player): Boolean =
        !Config.moonPhaseRequiresDial || moonPhaseDial.isPresent(player)

    @JvmStatic @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        pocketWatch.clear()
        moonPhaseDial.clear()
    }

    /** Check each required item at most once every five client ticks for the same player. */
    private class ItemPresence(private val item: () -> Item) {
        private var checkedPlayer: Player? = null
        private var checkedTick = 0
        private var present = false

        fun isPresent(player: Player): Boolean {
            val tick = player.tickCount
            if (checkedPlayer !== player || tick < checkedTick || tick - checkedTick >= 5) {
                val requiredItem = item()
                present = player.inventory.items.any { it.`is`(requiredItem) } ||
                    player.inventory.offhand.any { it.`is`(requiredItem) } ||
                    (curiosLoaded && CuriosItemIntegration.isEquipped(player, requiredItem))
                checkedPlayer = player
                checkedTick = tick
            }
            return present
        }

        fun clear() {
            checkedPlayer = null
        }
    }
}

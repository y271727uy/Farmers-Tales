package com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus

import com.y271727uy.farmerstales.FTMod
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Client copy of this dimension's dry overcast flag. */
object DarkCumulonimbusClientState {
    @JvmField
    @Volatile
    var active = false
}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object DarkCumulonimbusClientEvents {
    @JvmStatic
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        DarkCumulonimbusClientState.active = false
    }
}

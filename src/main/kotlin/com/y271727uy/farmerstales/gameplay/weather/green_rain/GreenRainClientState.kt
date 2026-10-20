package com.y271727uy.farmerstales.gameplay.weather.green_rain

import com.y271727uy.farmerstales.FTMod
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Client copy of whether this dimension's current storm is green rain.
 * Independent of later visual code so rain colour can subscribe separately.
 */
object GreenRainClientState {
    @JvmField
    @Volatile
    var active = false
}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object GreenRainClientEvents {
    @JvmStatic
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        GreenRainClientState.active = false
    }
}

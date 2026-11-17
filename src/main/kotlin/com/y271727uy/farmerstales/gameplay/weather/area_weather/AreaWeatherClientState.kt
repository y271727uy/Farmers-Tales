package com.y271727uy.farmerstales.gameplay.weather.area_weather

import com.y271727uy.farmerstales.FTMod
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Contains no client-only class references, so packet loading is safe on dedicated servers. */
object AreaWeatherClientState {
    var weather: AreaWeather? = null
}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object AreaWeatherClientEvents {
    @JvmStatic @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) { AreaWeatherClientState.weather = null }
}

package com.y271727uy.farmerstales.gameplay.weather.heavy_rain

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.weather.green_rain.GreenRainClientState
import com.y271727uy.farmerstales.integration.IntegrationManager
import net.minecraft.client.Minecraft
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Packet state stays free of client-only class references for dedicated-server loading. */
object HeavyRainClientState {
    var temperate = RainIntensity.NORMAL
    var tropical = RainIntensity.NORMAL
}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object HeavyRainClientVisuals {
    @JvmStatic
    fun densityMultiplier(): Float {
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return 1.0f
        if (!level.isRaining || GreenRainClientState.active) return 1.0f
        val pos = minecraft.gameRenderer.mainCamera.blockPosition
        val intensity = if (IntegrationManager.usesTropicalSeasons(level, pos)) {
            HeavyRainClientState.tropical
        } else HeavyRainClientState.temperate
        return when (intensity) {
            RainIntensity.NORMAL -> 1.0f
            RainIntensity.HEAVY -> 1.2f
            RainIntensity.TORRENTIAL -> 1.4f
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        HeavyRainClientState.temperate = RainIntensity.NORMAL
        HeavyRainClientState.tropical = RainIntensity.NORMAL
    }
}

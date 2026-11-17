package com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.client.SkyTint
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Dry overcast: rain-cloud colour without rain streaks. Real rain owns the sky
 * when both flags could be visible, so this tint only applies while it is clear.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object DarkCumulonimbusClientVisuals {
    private val TINT = SkyTint(0.16f, 0.17f, 0.20f)

    @JvmStatic
    fun tintSky(original: Vec3): Vec3 =
        if (DarkCumulonimbusClientState.active) TINT.mix(original, 0.72) else original

    @JvmStatic
    fun tintClouds(original: Vec3): Vec3 =
        if (DarkCumulonimbusClientState.active) TINT.mix(original, 0.78) else original

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!DarkCumulonimbusClientState.active) return
        if (event.camera.entity.level().isRaining) return
        TINT.mixFog(event, 0.62f)
    }
}

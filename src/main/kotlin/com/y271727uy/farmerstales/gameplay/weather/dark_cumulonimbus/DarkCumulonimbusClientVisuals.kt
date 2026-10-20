package com.y271727uy.farmerstales.gameplay.weather.dark_cumulonimbus

import com.y271727uy.farmerstales.FTMod
import net.minecraft.util.Mth
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
    private const val TINT_R = 0.16f
    private const val TINT_G = 0.17f
    private const val TINT_B = 0.20f
    private val TINT = Vec3(TINT_R.toDouble(), TINT_G.toDouble(), TINT_B.toDouble())

    @JvmStatic
    fun tintSky(original: Vec3): Vec3 =
        if (DarkCumulonimbusClientState.active) original.lerp(TINT, 0.72) else original

    @JvmStatic
    fun tintClouds(original: Vec3): Vec3 =
        if (DarkCumulonimbusClientState.active) original.lerp(TINT, 0.78) else original

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!DarkCumulonimbusClientState.active) return
        if (event.camera.entity.level().isRaining) return
        event.red = Mth.lerp(0.62f, event.red, TINT_R)
        event.green = Mth.lerp(0.62f, event.green, TINT_G)
        event.blue = Mth.lerp(0.62f, event.blue, TINT_B)
    }
}

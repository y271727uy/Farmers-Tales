package com.y271727uy.farmerstales.gameplay.weather.green_rain

import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Client visuals for an active green-rain storm: rain sheet swap plus a
 * yellow-green wash on fog, sky and clouds. No custom shader.
 *
 * Fog uses [EventPriority.LOW] so a night moon event still owns the tint
 * if both somehow fire.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object GreenRainClientVisuals {
    private val GREEN_RAIN = ResourceLocation.fromNamespaceAndPath(
        FTMod.MODID,
        "textures/environment/green_rain.png",
    )

    private const val TINT_R = 0.42f
    private const val TINT_G = 0.62f
    private const val TINT_B = 0.22f
    private val TINT = Vec3(TINT_R.toDouble(), TINT_G.toDouble(), TINT_B.toDouble())

    @JvmStatic
    fun activeRainTexture(): ResourceLocation? =
        if (GreenRainClientState.active) GREEN_RAIN else null

    @JvmStatic
    fun tintSky(original: Vec3): Vec3 =
        if (GreenRainClientState.active) original.lerp(TINT, 0.45) else original

    @JvmStatic
    fun tintClouds(original: Vec3): Vec3 =
        if (GreenRainClientState.active) original.lerp(TINT, 0.40) else original

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!GreenRainClientState.active) return
        event.red = Mth.lerp(0.55f, event.red, TINT_R)
        event.green = Mth.lerp(0.55f, event.green, TINT_G)
        event.blue = Mth.lerp(0.55f, event.blue, TINT_B)
    }
}

package com.y271727uy.farmerstales.gameplay.weather.green_rain

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.common.client.SkyTint
import net.minecraft.resources.ResourceLocation
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

    private val TINT = SkyTint(0.42f, 0.62f, 0.22f)

    @JvmStatic
    fun activeRainTexture(): ResourceLocation? =
        if (GreenRainClientState.active) GREEN_RAIN else null

    @JvmStatic
    fun tintSky(original: Vec3): Vec3 =
        if (GreenRainClientState.active) TINT.mix(original, 0.45) else original

    @JvmStatic
    fun tintClouds(original: Vec3): Vec3 =
        if (GreenRainClientState.active) TINT.mix(original, 0.40) else original

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOW)
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        if (!GreenRainClientState.active) return
        TINT.mixFog(event, 0.55f)
    }
}

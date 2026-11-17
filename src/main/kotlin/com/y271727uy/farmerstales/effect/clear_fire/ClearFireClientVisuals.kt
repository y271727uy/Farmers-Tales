package com.y271727uy.farmerstales.effect.clear_fire

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModEffects
import net.minecraft.client.Minecraft
import net.minecraft.world.level.material.FogType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object ClearFireClientVisuals {
    @JvmStatic
    @SubscribeEvent
    fun onRenderScreenEffect(event: RenderBlockScreenEffectEvent) {
        if (event.overlayType == RenderBlockScreenEffectEvent.OverlayType.FIRE &&
            event.player.hasEffect(ModEffects.CLEAR_FIRE.get())
        ) {
            event.isCanceled = true
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRenderFog(event: ViewportEvent.RenderFog) {
        if (event.type != FogType.LAVA) return

        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return
        if (!player.hasEffect(ModEffects.CLEAR_FIRE.get())) return

        // Match vanilla's spectator fog distances in lava.
        event.nearPlaneDistance = -8.0f
        event.farPlaneDistance = minecraft.gameRenderer.renderDistance * 0.5f
        event.isCanceled = true
    }
}

package com.y271727uy.farmerstales.gameplay.special_event.common

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.y271727uy.farmerstales.FTMod
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Client-only moon texture swap, fog tint and additive halo.
 * Driven by the synced world winner, not the HUD marker effect.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object MoonPhaseClientVisuals {
    private val MOON_HALO = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/environment/moon_halo.png")

    @JvmStatic
    fun activeMoonTexture(): ResourceLocation? = activeEvent()?.moonTexture

    @JvmStatic
    fun drawMoonHalo(poseStack: PoseStack) {
        val halo = activeEvent()?.haloColor ?: return
        val matrix = poseStack.last().pose()
        val r = ((halo shr 16) and 0xFF) / 255f
        val g = ((halo shr 8) and 0xFF) / 255f
        val b = (halo and 0xFF) / 255f
        val size = 30.0f

        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, MOON_HALO)
        RenderSystem.setShaderColor(r, g, b, 0.4f)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.vertex(matrix, -size, -100.0f, size).uv(0.0f, 0.0f).endVertex()
        builder.vertex(matrix, size, -100.0f, size).uv(1.0f, 0.0f).endVertex()
        builder.vertex(matrix, size, -100.0f, -size).uv(1.0f, 1.0f).endVertex()
        builder.vertex(matrix, -size, -100.0f, -size).uv(0.0f, 1.0f).endVertex()
        BufferUploader.drawWithShader(builder.end())
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
    }

    @JvmStatic
    @SubscribeEvent
    fun onFogColor(event: ViewportEvent.ComputeFogColor) {
        val color = activeEvent()?.fogColor ?: return
        event.red = ((color shr 16) and 0xFF) / 255f
        event.green = ((color shr 8) and 0xFF) / 255f
        event.blue = (color and 0xFF) / 255f
    }

    @JvmStatic
    @SubscribeEvent
    fun onLogout(event: ClientPlayerNetworkEvent.LoggingOut) {
        MoonPhaseClientState.winnerId = ""
    }

    private fun activeEvent(): MoonPhaseEvent? {
        val id = MoonPhaseClientState.winnerId
        if (id.isEmpty()) return null
        return MoonPhaseEvents.get(id)
    }
}

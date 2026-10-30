package com.y271727uy.farmerstales.gameplay.quality_crop.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

object QualityOverlayRenderer {
    private const val WORLD_OVERLAY_SIZE = 0.22f
    private const val WORLD_OVERLAY_Z_OFFSET = 0.01f

    @JvmStatic
    fun render(guiGraphics: GuiGraphics, stack: ItemStack, x: Int, y: Int) {
        render(guiGraphics, stack, x, y, 200.0)
    }

    @JvmStatic
    fun render(guiGraphics: GuiGraphics, stack: ItemStack, x: Int, y: Int, zOffset: Double) {
        val quality = QualityNbt.get(stack) ?: return

        val poseStack = guiGraphics.pose()
        poseStack.pushPose()
        poseStack.translate(-1.0, 0.0, zOffset)
        poseStack.translate((x + 8).toDouble(), (y + 8).toDouble(), 0.0)
        poseStack.scale(1.0f, 1.0f, 1.0f)
        guiGraphics.blit(quality.textureLocation, -8, -8, 0.0f, 0.0f, 16, 16, 16, 16)
        poseStack.popPose()
    }

    @JvmStatic
    @Suppress("unused")
    fun renderWorld(poseStack: PoseStack, buffer: MultiBufferSource, stack: ItemStack, displayContext: ItemDisplayContext) {
        if (!shouldRenderWorldOverlay(displayContext)) {
            return
        }

        val quality = QualityNbt.get(stack) ?: return

        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, WORLD_OVERLAY_Z_OFFSET.toDouble())

        val pose = poseStack.last()
        val consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(quality.textureLocation))
        val size = WORLD_OVERLAY_SIZE
        val matrix = pose.pose()
        val normal = pose.normal()

        consumer.vertex(matrix, -size, -size, 0.0f)
            .color(255, 255, 255, 255)
            .uv(0.0f, 1.0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0f, 0.0f, 1.0f)
            .endVertex()

        consumer.vertex(matrix, -size, size, 0.0f)
            .color(255, 255, 255, 255)
            .uv(0.0f, 0.0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0f, 0.0f, 1.0f)
            .endVertex()

        consumer.vertex(matrix, size, size, 0.0f)
            .color(255, 255, 255, 255)
            .uv(1.0f, 0.0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0f, 0.0f, 1.0f)
            .endVertex()

        consumer.vertex(matrix, size, -size, 0.0f)
            .color(255, 255, 255, 255)
            .uv(1.0f, 1.0f)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT)
            .normal(normal, 0.0f, 0.0f, 1.0f)
            .endVertex()

        poseStack.popPose()
    }

    private fun shouldRenderWorldOverlay(displayContext: ItemDisplayContext): Boolean {
        return when (displayContext) {
            ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
            ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
            ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
            ItemDisplayContext.GROUND -> true
            else -> false
        }
    }

}





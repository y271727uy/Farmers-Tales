package com.y271727uy.farmerstales.gameplay.fish_group.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.RiverFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import org.joml.Quaternionf

class RiverFishPoolRenderer(context: EntityRendererProvider.Context) : EntityRenderer<RiverFishPoolEntity>(context) {
    private val model = RiverFishPoolModel(context.bakeLayer(RiverFishPoolModel.LAYER_LOCATION))

    override fun getTextureLocation(entity: RiverFishPoolEntity): ResourceLocation = TEXTURE

    override fun render(
        entity: RiverFishPoolEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
    ) {
        poseStack.pushPose()
        poseStack.mulPose(Quaternionf().rotateX(Math.PI.toFloat()))
        poseStack.mulPose(Quaternionf().rotateY(Math.toRadians(entity.randomRotation.toDouble()).toFloat()))
        poseStack.translate(0.0, -3.5, 0.0)
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, entityYaw, 0.0F)
        val overlay = if (entity.hurtTime > 0) OverlayTexture.RED_OVERLAY_V else OverlayTexture.NO_OVERLAY
        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)),
            packedLight,
            overlay,
            1.0F,
            1.0F,
            1.0F,
            1.0F,
        )
        poseStack.popPose()
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/entity/river_fish_pool.png")
    }
}

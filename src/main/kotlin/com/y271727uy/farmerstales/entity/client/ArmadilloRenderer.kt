package com.y271727uy.farmerstales.entity.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.client.model.ArmadilloModel
import com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class ArmadilloRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<ArmadilloEntity, ArmadilloModel>(context, ArmadilloModel(context.bakeLayer(ArmadilloModel.LAYER_LOCATION)), 0.35F) {
    override fun getTextureLocation(entity: ArmadilloEntity): ResourceLocation = TEXTURE

    override fun scale(entity: ArmadilloEntity, poseStack: PoseStack, partialTickTime: Float) {
        if (entity.isBaby) poseStack.scale(0.5F, 0.5F, 0.5F)
    }

    companion object {
        private val TEXTURE = ResourceLocation(FTMod.MODID, "textures/entity/armadillo/armadillo.png")
    }
}

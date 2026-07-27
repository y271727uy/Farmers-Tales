package com.y271727uy.farmerstales.entity.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.client.model.RatModel
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class RatRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<RatEntity, RatModel>(context, RatModel(context.bakeLayer(RatModel.LAYER_LOCATION)), 0.2F) {
    override fun getTextureLocation(entity: RatEntity): ResourceLocation = TEXTURE

    override fun scale(entity: RatEntity, poseStack: PoseStack, partialTickTime: Float) {
        if (entity.isBaby) poseStack.scale(0.55F, 0.55F, 0.55F)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/entity/rat/rat.png")
    }
}

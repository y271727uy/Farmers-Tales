package com.y271727uy.farmerstales.entity.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.client.model.BambooRatModel
import com.y271727uy.farmerstales.entity.mobs.BambooRatEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class BambooRatRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<BambooRatEntity, BambooRatModel>(context, BambooRatModel(context.bakeLayer(BambooRatModel.LAYER_LOCATION)), 0.25F) {
    override fun getTextureLocation(entity: BambooRatEntity): ResourceLocation = TEXTURE

    override fun scale(entity: BambooRatEntity, poseStack: PoseStack, partialTickTime: Float) {
        if (entity.isBaby) poseStack.scale(0.55F, 0.55F, 0.55F)
    }

    companion object {
        private val TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FTMod.MODID,
            "textures/entity/bamboo_rat/bamboo_rat.png",
        )
    }
}

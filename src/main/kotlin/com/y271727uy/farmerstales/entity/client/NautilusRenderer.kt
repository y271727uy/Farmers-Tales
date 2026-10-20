package com.y271727uy.farmerstales.entity.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.client.model.NautilusModel
import com.y271727uy.farmerstales.entity.mobs.NautilusEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class NautilusRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<NautilusEntity, NautilusModel<NautilusEntity>>(
        context,
        NautilusModel(context.bakeLayer(NautilusModel.LAYER_LOCATION)),
        0.5F,
    ) {
    override fun getTextureLocation(entity: NautilusEntity): ResourceLocation = TEXTURE

    override fun scale(entity: NautilusEntity, poseStack: PoseStack, partialTickTime: Float) {
        if (entity.isBaby) poseStack.scale(0.55F, 0.55F, 0.55F)
    }

    companion object {
        private val TEXTURE = ResourceLocation(FTMod.MODID, "textures/entity/nautilus/nautilus.png")
    }
}

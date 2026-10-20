package com.y271727uy.farmerstales.entity.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.client.model.NautilusModel
import com.y271727uy.farmerstales.entity.mobs.ZombieNautilusEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.resources.ResourceLocation

class ZombieNautilusRenderer(context: EntityRendererProvider.Context) :
    MobRenderer<ZombieNautilusEntity, NautilusModel<ZombieNautilusEntity>>(
        context,
        NautilusModel(context.bakeLayer(NautilusModel.LAYER_LOCATION)),
        0.5F,
    ) {
    override fun getTextureLocation(entity: ZombieNautilusEntity): ResourceLocation = TEXTURE

    companion object {
        private val TEXTURE = ResourceLocation(FTMod.MODID, "textures/entity/nautilus/zombie_nautilus.png")
    }
}

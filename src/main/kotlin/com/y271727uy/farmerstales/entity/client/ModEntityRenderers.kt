package com.y271727uy.farmerstales.entity.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.ModEntities
import com.y271727uy.farmerstales.entity.client.model.BambooRatModel
import com.y271727uy.farmerstales.entity.client.model.RatModel
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModEntityRenderers {
    @JvmStatic
    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerEntityRenderer(ModEntities.BAMBOO_RAT.type.get(), ::BambooRatRenderer)
        event.registerEntityRenderer(ModEntities.RAT.type.get(), ::RatRenderer)
    }

    @JvmStatic
    @SubscribeEvent
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(BambooRatModel.LAYER_LOCATION, BambooRatModel::createBodyLayer)
        event.registerLayerDefinition(RatModel.LAYER_LOCATION, RatModel::createBodyLayer)
    }
}

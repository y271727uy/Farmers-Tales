package com.y271727uy.farmerstales.entity.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.ModEntities
import com.y271727uy.farmerstales.entity.client.model.BambooRatModel
import com.y271727uy.farmerstales.entity.client.model.ArmadilloModel
import com.y271727uy.farmerstales.entity.client.model.NautilusModel
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
        event.registerEntityRenderer(ModEntities.ARMADILLO.type.get(), ::ArmadilloRenderer)
        event.registerEntityRenderer(ModEntities.NAUTILUS.type.get(), ::NautilusRenderer)
        event.registerEntityRenderer(ModEntities.ZOMBIE_NAUTILUS.type.get(), ::ZombieNautilusRenderer)
        event.registerEntityRenderer(ModEntities.BAMBOO_RAT.type.get(), ::BambooRatRenderer)
        event.registerEntityRenderer(ModEntities.RAT.type.get(), ::RatRenderer)
    }

    @JvmStatic
    @SubscribeEvent
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(ArmadilloModel.LAYER_LOCATION, ArmadilloModel::createBodyLayer)
        event.registerLayerDefinition(NautilusModel.LAYER_LOCATION) { NautilusModel.createBodyLayer() }
        event.registerLayerDefinition(BambooRatModel.LAYER_LOCATION, BambooRatModel::createBodyLayer)
        event.registerLayerDefinition(RatModel.LAYER_LOCATION, RatModel::createBodyLayer)
    }
}

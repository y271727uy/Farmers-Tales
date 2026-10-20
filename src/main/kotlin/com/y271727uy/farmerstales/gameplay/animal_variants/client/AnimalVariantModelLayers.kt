package com.y271727uy.farmerstales.gameplay.animal_variants.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.client.model.ModernCowModel
import com.y271727uy.farmerstales.client.model.ModernColdCowModel
import com.y271727uy.farmerstales.client.model.ModernWarmCowModel
import com.y271727uy.farmerstales.client.model.ModernPigModel
import com.y271727uy.farmerstales.client.model.ModernColdPigModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object AnimalVariantModelLayers {
    @JvmField
    val MODERN_COW = ModelLayerLocation(ResourceLocation(FTMod.MODID, "modern_cow"), "main")
    @JvmField
    val MODERN_COLD_COW = ModelLayerLocation(ResourceLocation(FTMod.MODID, "modern_cold_cow"), "main")
    @JvmField
    val MODERN_WARM_COW = ModelLayerLocation(ResourceLocation(FTMod.MODID, "modern_warm_cow"), "main")
    @JvmField
    val MODERN_PIG = ModelLayerLocation(ResourceLocation(FTMod.MODID, "modern_pig"), "main")
    @JvmField
    val MODERN_COLD_PIG = ModelLayerLocation(ResourceLocation(FTMod.MODID, "modern_cold_pig"), "main")

    @JvmStatic
    @SubscribeEvent
    fun registerLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(MODERN_COW, ModernCowModel::createBodyLayer)
        event.registerLayerDefinition(MODERN_COLD_COW, ModernColdCowModel::createBodyLayer)
        event.registerLayerDefinition(MODERN_WARM_COW, ModernWarmCowModel::createBodyLayer)
        event.registerLayerDefinition(MODERN_PIG, ModernPigModel::createBodyLayer)
        event.registerLayerDefinition(MODERN_COLD_PIG, ModernColdPigModel::createBodyLayer)
    }
}

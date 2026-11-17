package com.y271727uy.farmerstales.gameplay.fish_group.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.OceanFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

class OceanFishPoolRenderer(context: EntityRendererProvider.Context) : FishPoolRenderer<OceanFishPoolEntity>(
    context,
    OceanFishPoolModel(context.bakeLayer(OceanFishPoolModel.LAYER_LOCATION)),
    ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/entity/ocean_fish_pool.png"),
)

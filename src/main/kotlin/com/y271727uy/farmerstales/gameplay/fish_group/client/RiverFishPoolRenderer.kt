package com.y271727uy.farmerstales.gameplay.fish_group.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.RiverFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation

class RiverFishPoolRenderer(context: EntityRendererProvider.Context) : FishPoolRenderer<RiverFishPoolEntity>(
    context,
    RiverFishPoolModel(context.bakeLayer(RiverFishPoolModel.LAYER_LOCATION)),
    ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "textures/entity/river_fish_pool.png"),
)

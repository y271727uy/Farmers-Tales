package com.y271727uy.farmerstales.gameplay.fish_group.client.model

import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.client.model.generators.CustomLoaderBuilder
import net.minecraftforge.client.model.generators.ModelBuilder
import net.minecraftforge.common.data.ExistingFileHelper

class HaloModelBuilder<T : ModelBuilder<T>>(parent: T, existingFileHelper: ExistingFileHelper) :
    CustomLoaderBuilder<T>(ResourceLocation.fromNamespaceAndPath("avaritia", "halo"), parent, existingFileHelper) {
    private var type = 1
    private var alpha = 0.6F

    fun type(type: Int) = apply { this.type = type }

    fun alpha(alpha: Float) = apply { this.alpha = alpha }

    override fun toJson(json: JsonObject): JsonObject = super.toJson(json).apply {
        add("halo", JsonObject().apply {
            addProperty("type", type)
            addProperty("alpha", alpha)
        })
    }
}

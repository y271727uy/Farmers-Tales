package com.y271727uy.farmerstales.data

import com.tterrag.registrate.providers.RegistrateItemModelProvider
import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraftforge.client.model.generators.ModelFile

object ModModels {
    fun initItem(provider: RegistrateItemModelProvider) {
        // Shared templates belong here; individual registrations still declare their model.
        provider.getBuilder("template_item_generated")
            .parent(ModelFile.UncheckedModelFile("item/generated"))
        ModEntities.generateItemModels(provider)
    }
}

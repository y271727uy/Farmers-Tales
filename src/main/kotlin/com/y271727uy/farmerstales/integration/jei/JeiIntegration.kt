package com.y271727uy.farmerstales.integration.jei

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.jei.category.FishPondStructureCategory
import com.y271727uy.farmerstales.integration.list.FishPondPattern
import mezz.jei.api.IModPlugin
import mezz.jei.api.JeiPlugin
import mezz.jei.api.recipe.RecipeType
import mezz.jei.api.registration.IRecipeCatalystRegistration
import mezz.jei.api.registration.IRecipeCategoryRegistration
import mezz.jei.api.registration.IRecipeRegistration
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.fml.ModList

object JeiIntegration {
    fun init() {
        FTMod.LOGGER.debug("JEI compatibility enabled")
    }
}

@JeiPlugin
class FarmerTalesJeiPlugin : IModPlugin {
    override fun getPluginUid(): ResourceLocation {
        return ResourceLocation(FTMod.MODID, "jei_plugin")
    }

    override fun registerCategories(registration: IRecipeCategoryRegistration) {
        if (!listLoaded()) {
            return
        }
        registration.addRecipeCategories(FishPondStructureCategory(registration.jeiHelpers.guiHelper))
    }

    override fun registerRecipes(registration: IRecipeRegistration) {
        if (!listLoaded()) {
            return
        }
        registration.addRecipes(
            FISH_POND_STRUCTURE,
            FishPondPattern.PAGES.map(::FishPondStructureRecipe),
        )
    }

    override fun registerRecipeCatalysts(registration: IRecipeCatalystRegistration) {
        val catalyst = FishPondStructureCategory.controllerStack()
        if (catalyst.isEmpty) {
            return
        }
        registration.addRecipeCatalyst(catalyst, FISH_POND_STRUCTURE)
    }

    private fun listLoaded(): Boolean {
        return ModList.get().isLoaded("list")
    }

    companion object {
        val FISH_POND_STRUCTURE: RecipeType<FishPondStructureRecipe> = RecipeType.create(
            FTMod.MODID,
            "fish_pond_structure",
            FishPondStructureRecipe::class.java,
        )
    }
}

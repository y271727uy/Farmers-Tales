package com.y271727uy.farmerstales.integration.jei.category

import com.lowdragmc.lowdraglib.jei.ModularUIRecipeCategory
import com.lowdragmc.lowdraglib.jei.ModularWrapper
import com.y271727uy.farmerstales.integration.jei.FishPondStructureRecipe
import com.y271727uy.farmerstales.integration.jei.FarmerTalesJeiPlugin
import com.y271727uy.farmerstales.integration.jei.widget.FishPondPreviewWidget
import com.y271727uy.farmerstales.integration.list.FishPondPattern
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder
import mezz.jei.api.gui.drawable.IDrawable
import mezz.jei.api.helpers.IGuiHelper
import mezz.jei.api.recipe.IFocusGroup
import mezz.jei.api.recipe.RecipeIngredientRole
import mezz.jei.api.recipe.RecipeType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.registries.BuiltInRegistries

class FishPondStructureCategory(guiHelper: IGuiHelper) : ModularUIRecipeCategory<FishPondStructureRecipe>(
    { recipe -> ModularWrapper(FishPondPreviewWidget.create(recipe)) },
) {
    private val icon: IDrawable = guiHelper.drawableBuilder(
        ResourceLocation("ldlib", "textures/gui/icon.png"),
        0, 0, 16, 16,
    ).setTextureSize(16, 16).build()

    override fun getRecipeType(): RecipeType<FishPondStructureRecipe> {
        return FarmerTalesJeiPlugin.FISH_POND_STRUCTURE
    }

    override fun getTitle(): Component {
        return Component.translatable("gui.farmerstales.jei.multiblock_info")
    }

    override fun getIcon(): IDrawable {
        return icon
    }

    override fun getWidth(): Int {
        return FishPondPreviewWidget.WIDTH
    }

    override fun getHeight(): Int {
        return FishPondPreviewWidget.HEIGHT
    }

    /**
     * 材料直接交给 JEI。ldlib 控件读材料的时机不稳定，按 U 会因此找不到这一页。
     */
    override fun setRecipe(builder: IRecipeLayoutBuilder, recipe: FishPondStructureRecipe, focuses: IFocusGroup) {
        super.setRecipe(builder, recipe, focuses)
        val materials = FishPondPattern.materials(recipe.pattern)
        materials.forEachIndexed { index, stack ->
            builder.addSlot(RecipeIngredientRole.INPUT, -1000, index)
                .addItemStack(stack)
                .setSlotName("structure_$index")
        }
        builder.addSlot(RecipeIngredientRole.CATALYST, -1000, materials.size)
            .addItemStack(controllerStack())
            .setSlotName("structure_catalyst")
    }

    companion object {
        fun controllerStack(): ItemStack {
            val block = BuiltInRegistries.BLOCK.get(ResourceLocation("list", "fishpond_core"))
            return if (block == Blocks.AIR) ItemStack.EMPTY else ItemStack(block)
        }
    }
}

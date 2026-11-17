package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.Potion
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.item.crafting.Ingredient
import net.minecraftforge.common.brewing.BrewingRecipeRegistry
import net.minecraftforge.common.crafting.PartialNBTIngredient
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModPotions {
    private val potions = DeferredRegister.create(ForgeRegistries.POTIONS, FTMod.MODID)

    val CLEAR_FIRE: RegistryObject<Potion> = potions.register("clear_fire") {
        Potion("${FTMod.MODID}.clear_fire", MobEffectInstance(ModEffects.CLEAR_FIRE.get(), 3 * 60 * 20))
    }

    fun register(bus: IEventBus) {
        potions.register(bus)
    }

    fun registerBrewingRecipes() {
        for (container in listOf(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION)) {
            val input = PotionUtils.setPotion(ItemStack(container), Potions.FIRE_RESISTANCE)
            val output = PotionUtils.setPotion(ItemStack(container), CLEAR_FIRE.get())
            BrewingRecipeRegistry.addRecipe(
                PartialNBTIngredient.of(container, input.orCreateTag),
                Ingredient.of(Items.CRIMSON_FUNGUS),
                output,
            )
        }
    }
}

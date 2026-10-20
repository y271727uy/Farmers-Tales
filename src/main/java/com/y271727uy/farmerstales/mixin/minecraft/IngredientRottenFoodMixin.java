package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.food_expiry.FoodExpiry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Makes marked rotten food fail every recipe using the standard Ingredient API. */
@Mixin(Ingredient.class)
public abstract class IngredientRottenFoodMixin {
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void farmerstales$rejectRottenFood(@Nullable ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (FoodExpiry.isRottenForRecipe(stack)) {
            cir.setReturnValue(false);
        }
    }
}

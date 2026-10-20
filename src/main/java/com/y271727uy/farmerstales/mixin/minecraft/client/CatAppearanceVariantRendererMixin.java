package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.renderer.entity.CatRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CatRenderer.class)
public class CatAppearanceVariantRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void farmerstales$getVariantTexture(Cat cat, CallbackInfoReturnable<ResourceLocation> cir) {
        String variant = AnimalAppearanceVariants.getVariant(cat);
        if (!variant.isEmpty()) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("cat", variant));
        }
    }
}

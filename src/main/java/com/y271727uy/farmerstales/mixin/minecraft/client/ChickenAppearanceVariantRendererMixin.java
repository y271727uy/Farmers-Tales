package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChickenRenderer.class)
public class ChickenAppearanceVariantRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void farmerstales$getVariantTexture(Chicken chicken, CallbackInfoReturnable<ResourceLocation> cir) {
        String variant = AnimalAppearanceVariants.getVariant(chicken);
        if (!variant.isEmpty()) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("chicken", variant));
        }
    }
}

package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfRenderer.class)
public class WolfAppearanceVariantRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void farmerstales$getVariantTexture(Wolf wolf, CallbackInfoReturnable<ResourceLocation> cir) {
        String variant = AnimalAppearanceVariants.getVariant(wolf);
        if (!variant.isEmpty()) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.wolf(wolf, variant));
        }
    }
}

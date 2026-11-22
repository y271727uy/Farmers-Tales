package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Sheep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SheepRenderer.class)
public class SheepAppearanceVariantRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Sheep;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void farmerstales$getCoatTexture(Sheep sheep, CallbackInfoReturnable<ResourceLocation> cir) {
        String coat = AnimalAppearanceVariants.getSheepCoat(sheep);
        if (coat != null) {
            String texture = sheep.isSheared() ? coat + "_sheared" : coat;
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("sheep", texture));
        }
    }
}

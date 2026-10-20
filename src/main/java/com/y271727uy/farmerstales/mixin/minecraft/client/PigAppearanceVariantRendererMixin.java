package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.client.PigVariantModelHolder;
import com.y271727uy.farmerstales.client.model.ModernColdPigModel;
import com.y271727uy.farmerstales.client.model.ModernPigModel;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import com.y271727uy.farmerstales.gameplay.animal_variants.client.AnimalVariantModelLayers;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PigRenderer.class)
public class PigAppearanceVariantRendererMixin implements PigVariantModelHolder {
    @Unique
    private ModernPigModel farmerstales$variantPigModel;
    @Unique
    private ModernColdPigModel farmerstales$coldPigModel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void farmerstales$createVariantPigModels(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.farmerstales$variantPigModel = new ModernPigModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_PIG));
        this.farmerstales$coldPigModel = new ModernColdPigModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_COLD_PIG));
    }

    @Override
    public PigModel<?> farmerstales$getVariantPigModel(String variant) {
        return "cold".equals(variant) ? this.farmerstales$coldPigModel : this.farmerstales$variantPigModel;
    }
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void farmerstales$getVariantTexture(Pig pig, CallbackInfoReturnable<ResourceLocation> cir) {
        String variant = AnimalAppearanceVariants.getVariant(pig);
        if (!variant.isEmpty()) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("pig", variant));
        }
    }
}

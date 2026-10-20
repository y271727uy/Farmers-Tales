package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.client.CowVariantModelHolder;
import com.y271727uy.farmerstales.client.model.ModernCowModel;
import com.y271727uy.farmerstales.client.model.ModernColdCowModel;
import com.y271727uy.farmerstales.client.model.ModernWarmCowModel;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import com.y271727uy.farmerstales.gameplay.animal_variants.client.AnimalVariantModelLayers;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CowRenderer.class)
public class CowAppearanceVariantRendererMixin implements CowVariantModelHolder {
    @Unique
    private ModernCowModel farmerstales$variantCowModel;
    @Unique
    private ModernColdCowModel farmerstales$coldCowModel;
    @Unique
    private ModernWarmCowModel farmerstales$warmCowModel;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void farmerstales$createVariantCowModel(EntityRendererProvider.Context context, CallbackInfo ci) {
        this.farmerstales$variantCowModel = new ModernCowModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_COW));
        this.farmerstales$coldCowModel = new ModernColdCowModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_COLD_COW));
        this.farmerstales$warmCowModel = new ModernWarmCowModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_WARM_COW));
    }

    @Override
    public CowModel<?> farmerstales$getVariantCowModel(String variant) {
        return switch (variant) {
            case "cold" -> this.farmerstales$coldCowModel;
            case "warm" -> this.farmerstales$warmCowModel;
            default -> this.farmerstales$variantCowModel;
        };
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void farmerstales$getVariantTexture(Cow cow, CallbackInfoReturnable<ResourceLocation> cir) {
        String variant = AnimalAppearanceVariants.getVariant(cow);
        if (!variant.isEmpty()) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("cow", variant));
        }
    }
}

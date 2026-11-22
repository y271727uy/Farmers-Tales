package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Rabbit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RabbitRenderer.class)
public class RabbitAppearanceVariantRendererMixin {
    @Inject(method = "getTextureLocation(Lnet/minecraft/world/entity/animal/Rabbit;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("HEAD"), cancellable = true)
    private void farmerstales$getCoatTexture(Rabbit rabbit, CallbackInfoReturnable<ResourceLocation> cir) {
        String coat = AnimalAppearanceVariants.getRabbitCoat(rabbit);
        if (coat != null) {
            cir.setReturnValue(AnimalAppearanceVariantTextures.farmAnimal("rabbit", coat));
        }
    }
}

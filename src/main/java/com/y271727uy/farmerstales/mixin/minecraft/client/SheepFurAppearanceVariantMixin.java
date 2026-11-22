package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.farmerstales.client.AnimalAppearanceVariantTextures;
import com.y271727uy.farmerstales.entity.AnimalMutationEntities;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.SheepFurLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(SheepFurLayer.class)
public abstract class SheepFurAppearanceVariantMixin {
    private static final String RENDER = "render(Lcom/mojang/blaze3d/vertex/PoseStack;"
            + "Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/animal/Sheep;FFFFFF)V";
    @Unique
    private static final float[] FARMERSTALES_BAKED_FUR_COLOR = {1.0F, 1.0F, 1.0F};

    @ModifyArg(
            method = RENDER, index = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/layers/SheepFurLayer;"
                    + "coloredCutoutModelCopyLayerRender(Lnet/minecraft/client/model/EntityModel;"
                    + "Lnet/minecraft/client/model/EntityModel;Lnet/minecraft/resources/ResourceLocation;"
                    + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
                    + "ILnet/minecraft/world/entity/LivingEntity;FFFFFFFFF)V"))
    private ResourceLocation farmerstales$applyCoatTexture(
            EntityModel<LivingEntity> parentModel, EntityModel<LivingEntity> furModel, ResourceLocation texture,
            PoseStack pose, MultiBufferSource buffers, int packedLight, LivingEntity entity,
            float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch,
            float partialTick, float red, float green, float blue
    ) {
        Sheep sheep = (Sheep) entity;
        if (sheep.hasCustomName() && "jeb_".equals(sheep.getName().getString())) return texture;
        AnimalMutationEntities.Appearance mutation = AnimalMutationEntities.appearance(sheep.getType());
        if (mutation != null) return mutation.getFur();
        if (sheep.getColor() == DyeColor.WHITE) {
            String coat = AnimalAppearanceVariants.getSheepCoat(sheep);
            if (coat != null) return AnimalAppearanceVariantTextures.farmAnimal("sheep", coat + "_fur");
        }
        return texture;
    }

    @ModifyVariable(method = RENDER, at = @At("STORE"), ordinal = 0,
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/Sheep;getColor()Lnet/minecraft/world/item/DyeColor;")))
    private float[] farmerstales$keepBakedFurColors(
            float[] dye, PoseStack pose, MultiBufferSource buffers, int light, Sheep sheep
    ) {
        return AnimalMutationEntities.appearance(sheep.getType()) != null
                ? FARMERSTALES_BAKED_FUR_COLOR : dye;
    }
}

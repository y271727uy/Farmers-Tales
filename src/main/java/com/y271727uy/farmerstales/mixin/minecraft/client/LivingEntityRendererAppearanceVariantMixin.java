package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.farmerstales.client.CowVariantModelHolder;
import com.y271727uy.farmerstales.client.PigVariantModelHolder;
import com.y271727uy.farmerstales.gameplay.animal_variants.AnimalAppearanceVariants;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the modern cow model only for entities that have one of our cosmetic variants. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererAppearanceVariantMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Unique
    private EntityModel<?> farmerstales$originalModel;

    @Inject(method = "render", at = @At("HEAD"))
    private void farmerstales$useVariantCowModel(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        String variant;
        if (entity instanceof Cow cow && (Object) this instanceof CowRenderer) {
            variant = AnimalAppearanceVariants.getVariant(cow);
        } else if (entity instanceof Pig pig && (Object) this instanceof PigRenderer) {
            variant = AnimalAppearanceVariants.getVariant(pig);
        } else return;
        if (variant.isEmpty() || AnimalAppearanceVariants.usesLegacyModel(variant)) return;

        this.farmerstales$originalModel = this.model;
        this.model = this.farmerstales$variantModel(entity, variant);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void farmerstales$restoreOriginalCowModel(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            CallbackInfo ci
    ) {
        if (this.farmerstales$originalModel != null) {
            this.model = this.farmerstales$originalModel();
            this.farmerstales$originalModel = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Unique
    private M farmerstales$variantModel(T entity, String variant) {
        if (entity instanceof Cow) {
            return (M) ((CowVariantModelHolder) (Object) this).farmerstales$getVariantCowModel(variant);
        }
        return (M) ((PigVariantModelHolder) (Object) this).farmerstales$getVariantPigModel(variant);
    }

    @SuppressWarnings("unchecked")
    @Unique
    private M farmerstales$originalModel() {
        return (M) this.farmerstales$originalModel;
    }
}

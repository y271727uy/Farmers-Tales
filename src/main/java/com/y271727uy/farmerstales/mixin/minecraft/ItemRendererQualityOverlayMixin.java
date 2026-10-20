package com.y271727uy.farmerstales.mixin.minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.farmerstales.gameplay.quality_crop.client.QualityOverlayRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.client.renderer.entity.ItemRenderer.class)
public abstract class ItemRendererQualityOverlayMixin {
    @Inject(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V", at = @At("TAIL"))
    private void farmerstales$renderQualityOverlay(ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, BakedModel bakedModel, CallbackInfo ci) {
        QualityOverlayRenderer.renderWorld(poseStack, buffer, stack, displayContext);
    }
}

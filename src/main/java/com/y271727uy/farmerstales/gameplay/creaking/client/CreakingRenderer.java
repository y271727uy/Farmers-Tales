package com.y271727uy.farmerstales.gameplay.creaking.client;
import com.mojang.blaze3d.vertex.PoseStack;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.gameplay.creaking.*;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CreakingRenderer extends MobRenderer<CreakingEntity, CreakingModel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(new ResourceLocation(FTMod.MODID, "creaking"), "main");
    private static final ResourceLocation TEXTURE = new ResourceLocation(FTMod.MODID, "textures/entity/creaking/creaking.png");
    private static final ResourceLocation EYES = new ResourceLocation(FTMod.MODID, "textures/entity/creaking/creaking_eyes.png");
    public CreakingRenderer(EntityRendererProvider.Context context) {
        super(context, new CreakingModel(context.bakeLayer(LAYER)), 0.6F);
        addLayer(new RenderLayer<>(this) {
            @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, CreakingEntity entity,
                    float swing, float speed, float partial, float age, float yaw, float pitch) {
                if (entity.isActivated() || entity.isTearingDown()) getParentModel().renderToBuffer(pose,
                        buffers.getBuffer(RenderType.eyes(EYES)), 15728640, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
            }
        });
    }
    @Override public ResourceLocation getTextureLocation(CreakingEntity entity) { return TEXTURE; }
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) { event.registerLayerDefinition(LAYER, CreakingModel::createBodyLayer); }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(CreakingContent.CREAKING.get(), CreakingRenderer::new); }
}

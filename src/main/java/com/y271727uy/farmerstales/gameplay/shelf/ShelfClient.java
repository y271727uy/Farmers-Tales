package com.y271727uy.farmerstales.gameplay.shelf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.y271727uy.farmerstales.FTMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=FTMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ShelfClient {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(ShelfContent.SHELF.get(),Renderer::new);}
    private static final class Renderer implements BlockEntityRenderer<ShelfBlockEntity> {
        private final net.minecraft.client.renderer.entity.ItemRenderer items;
        Renderer(BlockEntityRendererProvider.Context context){items=context.getItemRenderer();}
        @Override public void render(ShelfBlockEntity shelf,float partialTick,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
            float rotation=-shelf.getBlockState().getValue(ShelfBlock.FACING).toYRot();
            for(int slot=0;slot<3;slot++){
                var stack=shelf.getItem(slot);if(stack.isEmpty())continue;
                var model=items.getModel(stack,shelf.getLevel(),null,(int)shelf.getBlockPos().asLong()+slot);
                poses.pushPose();poses.translate(0.5,0.5,0.5);poses.mulPose(Axis.YP.rotationDegrees(rotation));
                poses.translate((slot-1)*0.3125,shelf.alignItemsToBottom()?-0.125:0,-0.25);poses.scale(0.25F,0.25F,0.25F);
                // 1.20 has no ON_SHELF context: compensate the vanilla block FIXED transform.
                if(model.isGui3d()){poses.mulPose(Axis.YP.rotationDegrees(180));poses.scale(2,2,2);}
                items.renderStatic(stack,ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,poses,buffers,shelf.getLevel(),(int)shelf.getBlockPos().asLong()+slot);
                poses.popPose();
            }
        }
    }
}

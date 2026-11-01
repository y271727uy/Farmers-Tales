package com.y271727uy.farmerstales.gameplay.fauna.client;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.gameplay.fauna.*;
import com.y271727uy.farmerstales.variant.WolfArmorHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=FTMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FaunaClient {
    public static final ModelLayerLocation ARMADILLO=new ModelLayerLocation(id("armadillo"),"main"),ARMOR=new ModelLayerLocation(id("wolf_armor"),"main");
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(FTMod.MODID,path);}
    @SubscribeEvent public static void definitions(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(ARMADILLO,ArmadilloModel::createBodyLayer);e.registerLayerDefinition(ARMOR,WolfArmorModel::createArmorLayer);}
    @SubscribeEvent public static void renderer(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(FaunaContent.ARMADILLO.get(),context->new MobRenderer<ArmadilloEntity,ArmadilloModel>(context,new ArmadilloModel(context.bakeLayer(ARMADILLO)),0.4F){
        @Override public ResourceLocation getTextureLocation(ArmadilloEntity entity){return id("textures/entity/armadillo/armadillo.png");}
        @Override protected void scale(ArmadilloEntity entity,PoseStack pose,float partial){if(entity.isBaby())pose.scale(0.5F,0.5F,0.5F);}
    });}
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){
        LivingEntityRenderer<Wolf,WolfModel<Wolf>> renderer=e.getRenderer(EntityType.WOLF);
        if(renderer!=null)renderer.addLayer(new RenderLayer<Wolf,WolfModel<Wolf>>(renderer){
            private final WolfArmorModel model=new WolfArmorModel(e.getEntityModels().bakeLayer(ARMOR));
            private void draw(PoseStack pose,MultiBufferSource buffers,int light,ResourceLocation texture,float red,float green,float blue){model.renderToBuffer(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,OverlayTexture.NO_OVERLAY,red,green,blue,1);}
            @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,Wolf wolf,float swing,float speed,float partial,float age,float yaw,float pitch){
                var armor=((WolfArmorHolder)wolf).farmerstales$getWolfArmor();if(armor.isEmpty()||wolf.isBaby()||wolf.isInvisible())return;
                getParentModel().copyPropertiesTo(model);model.prepareMobModel(wolf,swing,speed,partial);model.setupAnim(wolf,swing,speed,age,yaw,pitch);
                draw(pose,buffers,light,id("textures/entity/equipment/wolf_body/armadillo_scute.png"),1,1,1);
                var dye=(DyeableLeatherItem)armor.getItem();if(dye.hasCustomColor(armor)){int color=dye.getColor(armor);draw(pose,buffers,light,id("textures/entity/equipment/wolf_body/armadillo_scute_overlay.png"),((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F);}
                float remaining=1-armor.getDamageValue()/(float)armor.getMaxDamage();String crack=remaining<0.32?"high":remaining<0.69?"medium":remaining<0.95?"low":null;
                if(crack!=null)draw(pose,buffers,light,id("textures/entity/wolf/wolf_armor_crackiness_"+crack+".png"),1,1,1);
            }
        });
    }
    @SubscribeEvent public static void color(RegisterColorHandlersEvent.Item e){e.register((stack,tint)->tint==1&&((DyeableLeatherItem)stack.getItem()).hasCustomColor(stack)?((DyeableLeatherItem)stack.getItem()).getColor(stack):0xFFFFFF,FaunaContent.WOLF_ARMOR.get());}
}

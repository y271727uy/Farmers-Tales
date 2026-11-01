package com.y271727uy.farmerstales.gameplay.environment.forest;
import com.y271727uy.farmerstales.FTMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid=FTMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ForestTravelClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->ForestTravel.WOODS.values().forEach(Sheets::addWoodType));}
    @SubscribeEvent @SuppressWarnings("unchecked") public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ForestTravel.SIGN.get(),SignRenderer::new);
        event.registerBlockEntityRenderer(ForestTravel.HANGING_SIGN.get(),HangingSignRenderer::new);
        ForestTravel.BOATS.forEach((name,type)->event.registerEntityRenderer((EntityType<Boat>)type.get(),context->new Renderer(context,name)));
    }
    private static final class Renderer extends BoatRenderer {
        private final Pair<ResourceLocation,ListModel<Boat>> resource;
        Renderer(EntityRendererProvider.Context context,String name){
            super(context,name.endsWith("chest_boat"));boolean chest=name.endsWith("chest_boat");
            var part=context.bakeLayer(chest?ModelLayers.createChestBoatModelName(Boat.Type.OAK):ModelLayers.createBoatModelName(Boat.Type.OAK));
            String wood=name.startsWith("pale_oak")?"pale_oak":"poplar";
            resource=Pair.of(ResourceLocation.fromNamespaceAndPath(FTMod.MODID,"textures/entity/"+(chest?"chest_boat/":"boat/")+wood+".png"),chest?new ChestBoatModel(part):new BoatModel(part));
        }
        @Override public Pair<ResourceLocation,ListModel<Boat>> getModelWithLocation(Boat boat){return resource;}
    }
}

package com.y271727uy.farmerstales.gameplay.building;
import com.y271727uy.farmerstales.FTMod;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=FTMod.MODID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BuildingClient {
    @SubscribeEvent public static void particles(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(BuildingContent.PAUSE.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new GrowthParticle(level,x,y,z,dx,dy,dz,sprites,false));
        event.registerSpriteSet(BuildingContent.RESET.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new GrowthParticle(level,x,y,z,dx,dy,dz,sprites,true));
    }
    private static final class GrowthParticle extends TextureSheetParticle {
        GrowthParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean reset){
            super(level,x,y,z);pickSprite(sprites);xd=dx;yd=dy+(reset?0.03:-0.03);zd=dz;gravity=0;quadSize*=random.nextFloat()*0.6F+0.5F;lifetime=8;
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_OPAQUE;}
    }
}

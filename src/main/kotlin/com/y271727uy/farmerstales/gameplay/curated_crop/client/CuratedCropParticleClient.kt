package com.y271727uy.farmerstales.gameplay.curated_crop.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.curated_crop.CuratedCropParticles
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterParticleProvidersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object CuratedCropParticleClient {
    @JvmStatic
    @SubscribeEvent
    fun register(event: RegisterParticleProvidersEvent) {
        event.registerSpriteSet(CuratedCropParticles.MOON.get()) { sprites ->
            CuratedCropParticle.Provider(sprites, CuratedCropParticle.Kind.MOON)
        }
        event.registerSpriteSet(CuratedCropParticles.RAINBOW.get()) { sprites ->
            CuratedCropParticle.Provider(sprites, CuratedCropParticle.Kind.RAINBOW)
        }
        event.registerSpriteSet(CuratedCropParticles.POPULAR.get()) { sprites ->
            CuratedCropParticle.Provider(sprites, CuratedCropParticle.Kind.POPULAR)
        }
        event.registerSpriteSet(CuratedCropParticles.STARS.get()) { sprites ->
            CuratedCropParticle.Provider(sprites, CuratedCropParticle.Kind.STARS)
        }
    }
}

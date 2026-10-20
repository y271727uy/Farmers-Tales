package com.y271727uy.farmerstales.gameplay.curated_crop

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object CuratedCropParticles {
    val REGISTRY: DeferredRegister<ParticleType<*>> = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FTMod.MODID)

    val MOON: RegistryObject<SimpleParticleType> = REGISTRY.register("curated_crop_moon") { SimpleParticleType(false) }
    val RAINBOW: RegistryObject<SimpleParticleType> = REGISTRY.register("curated_crop_rainbow") { SimpleParticleType(false) }
    val POPULAR: RegistryObject<SimpleParticleType> = REGISTRY.register("curated_crop_popular") { SimpleParticleType(false) }
    val STARS: RegistryObject<SimpleParticleType> = REGISTRY.register("curated_crop_stars") { SimpleParticleType(false) }

    fun register(bus: net.minecraftforge.eventbus.api.IEventBus) {
        REGISTRY.register(bus)
    }
}

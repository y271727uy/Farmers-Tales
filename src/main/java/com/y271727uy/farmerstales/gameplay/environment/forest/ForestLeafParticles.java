package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Providers and emitters live exclusively on the client; registry IDs exist on both sides. */
public final class ForestLeafParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FTMod.MODID);
    /** Falling leaves under vanilla trees (Spring to Life, 1.21.5). */
    public static final RegistryObject<SimpleParticleType> TINTED = type("tinted_leaves", com.y271727uy.farmerstales.backport.BackportFeature.SPRING_PLANTS);
    public static final RegistryObject<SimpleParticleType> PALE_OAK = type("pale_oak_leaves", com.y271727uy.farmerstales.backport.BackportFeature.PALE_GARDEN);
    public static final RegistryObject<SimpleParticleType> RED_POPLAR = type("red_poplar_leaves", com.y271727uy.farmerstales.backport.BackportFeature.POPLAR);
    public static final RegistryObject<SimpleParticleType> ORANGE_POPLAR = type("orange_poplar_leaves", com.y271727uy.farmerstales.backport.BackportFeature.POPLAR);
    public static final RegistryObject<SimpleParticleType> YELLOW_POPLAR = type("yellow_poplar_leaves", com.y271727uy.farmerstales.backport.BackportFeature.POPLAR);

    private ForestLeafParticles() {}

    private static RegistryObject<SimpleParticleType> type(String name) {
        return TYPES.register(name, () -> new SimpleParticleType(false));
    }

    /** Absent (never registered) when {@code feature} is disabled; check {@code isPresent()}. */
    private static RegistryObject<SimpleParticleType> type(String name, com.y271727uy.farmerstales.backport.BackportFeature feature) {
        return feature.isEnabled() ? type(name)
                : RegistryObject.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(FTMod.MODID, name), ForgeRegistries.PARTICLE_TYPES);
    }

    public static void register(IEventBus bus) { TYPES.register(bus); }
}

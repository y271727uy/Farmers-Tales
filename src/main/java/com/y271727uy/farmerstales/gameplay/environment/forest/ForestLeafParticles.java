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
    public static final RegistryObject<SimpleParticleType> TINTED = type("tinted_leaves");
    public static final RegistryObject<SimpleParticleType> PALE_OAK = type("pale_oak_leaves");
    public static final RegistryObject<SimpleParticleType> RED_POPLAR = type("red_poplar_leaves");
    public static final RegistryObject<SimpleParticleType> ORANGE_POPLAR = type("orange_poplar_leaves");
    public static final RegistryObject<SimpleParticleType> YELLOW_POPLAR = type("yellow_poplar_leaves");

    private ForestLeafParticles() {}

    private static RegistryObject<SimpleParticleType> type(String name) {
        return TYPES.register(name, () -> new SimpleParticleType(false));
    }

    public static void register(IEventBus bus) { TYPES.register(bus); }
}

package com.y271727uy.farmerstales.gameplay.environment;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SpringNatureEffects {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, FTMod.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FTMod.MODID);
    public static final RegistryObject<SimpleParticleType> FIREFLY = PARTICLES.register("firefly", () -> new SimpleParticleType(false));
    public static final RegistryObject<SoundEvent> FIREFLY_BUSH_IDLE = SOUNDS.register("block.firefly_bush.idle",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "block.firefly_bush.idle")));

    private SpringNatureEffects() {}
    public static void register(IEventBus bus) { PARTICLES.register(bus); SOUNDS.register(bus); }
}

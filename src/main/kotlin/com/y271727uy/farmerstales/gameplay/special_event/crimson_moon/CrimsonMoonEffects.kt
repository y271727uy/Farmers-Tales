package com.y271727uy.farmerstales.gameplay.special_event.crimson_moon

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseMarkerEffect
import net.minecraft.world.effect.MobEffect
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object CrimsonMoonEffects {
    private val effects = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FTMod.MODID)

    @JvmField
    val CRIMSON_MOON: RegistryObject<MobEffect> =
        effects.register("crimson_moon") { MoonPhaseMarkerEffect(0x8B1A1A) }

    fun register(bus: IEventBus) {
        effects.register(bus)
        CrimsonMoonEvents.init()
    }
}

package com.y271727uy.farmerstales.gameplay.special_event.fertile_moon

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseMarkerEffect
import net.minecraft.world.effect.MobEffect
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object FertileMoonEffects {
    private val effects = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FTMod.MODID)

    @JvmField
    val FERTILE_MOON: RegistryObject<MobEffect> =
        effects.register("fertile_moon") { MoonPhaseMarkerEffect(0xD8B4FE) }

    fun register(bus: IEventBus) {
        effects.register(bus)
        FertileMoonEvents.init()
    }
}

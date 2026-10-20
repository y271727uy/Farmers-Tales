package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModSounds {
    private val sounds = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, FTMod.MODID)

    @JvmField
    val ROOSTER_CROW: RegistryObject<SoundEvent> = register("ambient.rooster.crow")

    @JvmField
    val WOLF_HOWL: RegistryObject<SoundEvent> = register("ambient.wolf.howl")

    fun register(bus: IEventBus) {
        sounds.register(bus)
    }

    private fun register(name: String): RegistryObject<SoundEvent> =
        sounds.register(name) { SoundEvent.createVariableRangeEvent(ResourceLocation(FTMod.MODID, name)) }
}

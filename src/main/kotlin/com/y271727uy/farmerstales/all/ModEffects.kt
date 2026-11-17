package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModEffects {
    private val effects = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, FTMod.MODID)

    val CLEAR_FIRE: RegistryObject<MobEffect> = effects.register("clear_fire") {
        object : MobEffect(MobEffectCategory.BENEFICIAL, 0xFF6A00) {}
    }

    fun register(bus: IEventBus) {
        effects.register(bus)
    }
}

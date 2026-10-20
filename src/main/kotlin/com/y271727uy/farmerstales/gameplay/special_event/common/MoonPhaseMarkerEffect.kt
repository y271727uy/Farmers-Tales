package com.y271727uy.farmerstales.gameplay.special_event.common

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.item.ItemStack

/** HUD marker shown while a moon-phase event is active. Milk cannot cancel it. */
class MoonPhaseMarkerEffect(color: Int) : MobEffect(MobEffectCategory.BENEFICIAL, color) {
    override fun getCurativeItems(): MutableList<ItemStack> = mutableListOf()
}

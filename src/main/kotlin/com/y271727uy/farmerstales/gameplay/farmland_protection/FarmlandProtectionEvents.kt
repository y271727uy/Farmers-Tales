package com.y271727uy.farmerstales.gameplay.farmland_protection

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModEnchantments
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/** Jumping and other landings turn farmland to dirt through this event. Boots with the enchantment skip that. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object FarmlandProtectionEvents {
    @JvmStatic
    @SubscribeEvent
    fun onFarmlandTrample(event: BlockEvent.FarmlandTrampleEvent) {
        val living = event.entity as? LivingEntity ?: return
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FARMLAND_PROTECTION.get(), living) > 0) {
            event.isCanceled = true
        }
    }
}

package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModEnchantments {
    private val enchantments = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, FTMod.MODID)

    @JvmField
    val FARMLAND_PROTECTION: RegistryObject<Enchantment> =
        enchantments.register("farmland_protection") { FarmlandProtectionEnchantment() }

    fun register(bus: IEventBus) {
        enchantments.register(bus)
    }
}

private class FarmlandProtectionEnchantment : Enchantment(
    Rarity.UNCOMMON,
    EnchantmentCategory.ARMOR_FEET,
    arrayOf(EquipmentSlot.FEET),
) {
    override fun getMaxLevel(): Int = 1

    override fun getMinCost(level: Int): Int = 5

    override fun getMaxCost(level: Int): Int = getMinCost(level) + 20
}

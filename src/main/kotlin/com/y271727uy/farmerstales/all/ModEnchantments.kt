package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModEnchantments {
    private val enchantments = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, FTMod.MODID)

    @JvmField
    val FARMLAND_PROTECTION: RegistryObject<Enchantment> =
        enchantments.register("farmland_protection") { FarmlandProtectionEnchantment() }

    @JvmField
    val FISHING_FRENZY: RegistryObject<Enchantment> =
        enchantments.register("fishing_frenzy") { FishingPaceEnchantment(curse = false) }

    @JvmField
    val FISH_KING_CATCHER: RegistryObject<Enchantment> =
        enchantments.register("fish_king_catcher") { FishKingCatcherEnchantment() }

    @JvmField
    val FOCUSED_FISHING: RegistryObject<Enchantment> =
        enchantments.register("focused_fishing") { FocusedFishingEnchantment() }

    @JvmField
    val CURSE_OF_SALTY_BAIT: RegistryObject<Enchantment> =
        enchantments.register("curse_of_salty_bait") { FishingPaceEnchantment(curse = true) }

    @JvmField
    val CURSE_OF_THE_SEA: RegistryObject<Enchantment> =
        enchantments.register("curse_of_the_sea") { CurseOfTheSeaEnchantment() }

    fun register(bus: IEventBus) {
        enchantments.register(bus)
    }
}

private class CurseOfTheSeaEnchantment : Enchantment(
    Rarity.RARE,
    EnchantmentCategory.FISHING_ROD,
    arrayOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
) {
    override fun getMaxLevel(): Int = 3

    override fun isCurse(): Boolean = true

    override fun isTreasureOnly(): Boolean = true

    override fun checkCompatibility(other: Enchantment): Boolean =
        super.checkCompatibility(other) && other != Enchantments.FISHING_LUCK
}

private class FocusedFishingEnchantment : Enchantment(
    Rarity.RARE,
    EnchantmentCategory.FISHING_ROD,
    arrayOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
) {
    override fun getMinCost(level: Int): Int = 15

    override fun getMaxCost(level: Int): Int = 45
}

private class FishingPaceEnchantment(private val curse: Boolean) : Enchantment(
    Rarity.RARE,
    EnchantmentCategory.FISHING_ROD,
    arrayOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
) {
    override fun isCurse(): Boolean = curse

    override fun isTreasureOnly(): Boolean = curse

    override fun getMinCost(level: Int): Int = 15

    override fun getMaxCost(level: Int): Int = 45

    override fun checkCompatibility(other: Enchantment): Boolean =
        super.checkCompatibility(other) && other !is FishingPaceEnchantment
}

private class FishKingCatcherEnchantment : Enchantment(
    Rarity.VERY_RARE,
    EnchantmentCategory.FISHING_ROD,
    arrayOf(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND),
) {
    override fun getMaxLevel(): Int = 3

    override fun isTreasureOnly(): Boolean = true

    override fun isTradeable(): Boolean = false

    override fun isDiscoverable(): Boolean = false
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

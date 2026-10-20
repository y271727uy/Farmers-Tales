@file:Suppress("unused")

package com.y271727uy.farmerstales.gameplay.quality_crop

import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.util.RandomSource

/**
 * quality NBT 读写与校验入口。
 */
object QualityNbt {
    const val LEGACY_QUALITY_KEY: String = "quality"

    private val QUALITY_ORDER: List<Quality> = Quality.values().sortedByDescending { it.id }.toList()

    @JvmStatic
    fun parse(value: Int): Quality? = Quality.fromId(value)

    @JvmStatic
    fun parse(tag: CompoundTag?): Quality? {
        if (tag == null) {
            return null
        }

        for (quality in QUALITY_ORDER) {
            if (hasQualityFlag(tag, quality.nbtKey)) {
                return quality
            }
        }

        if (!tag.contains(LEGACY_QUALITY_KEY)) {
            return null
        }

        return parse(tag.getInt(LEGACY_QUALITY_KEY))
    }

    @JvmStatic
    fun get(stack: ItemStack): Quality? = parse(stack.tag)

    @JvmStatic
    fun getQualityId(stack: ItemStack): Int? = get(stack)?.id

    @JvmStatic
    fun hasValidQuality(stack: ItemStack): Boolean = get(stack) != null

    @JvmStatic
    fun getMinPriceBonus(stack: ItemStack): Int = get(stack)?.priceBonusRange?.first ?: 0

    @JvmStatic
    fun getMaxPriceBonus(stack: ItemStack): Int = get(stack)?.priceBonusRange?.last ?: 0

    @JvmStatic
    fun rollPriceBonus(stack: ItemStack, random: RandomSource): Int {
        val quality = get(stack) ?: return 0
        val min = quality.priceBonusRange.first
        val max = quality.priceBonusRange.last
        return if (max <= min) min else min + random.nextInt(max - min + 1)
    }

    @JvmStatic
    fun write(tag: CompoundTag, quality: Quality): CompoundTag {
        clear(tag)
        tag.putBoolean(quality.nbtKey, true)
        return tag
    }

    @JvmStatic
    fun write(stack: ItemStack, quality: Quality): ItemStack {
        write(stack.orCreateTag, quality)
        return stack
    }

    @JvmStatic
    fun write(stack: ItemStack, quality: Quality, harvester: Player?): ItemStack {
        write(stack, quality)
        (harvester as? ServerPlayer)?.let {
            FarmersTalesAdvancements.award(it, "quality_crop/first_quality_crop", quality.name.lowercase(java.util.Locale.ROOT))
        }
        return stack
    }

    @JvmStatic
    fun clear(stack: ItemStack) {
        val tag = stack.tag ?: return
        clear(tag)
    }

    @JvmStatic
    fun clear(tag: CompoundTag) {
        for (quality in Quality.values()) {
            tag.remove(quality.nbtKey)
        }
        tag.remove(LEGACY_QUALITY_KEY)
    }

    private fun hasQualityFlag(tag: CompoundTag, key: String): Boolean {
        if (!tag.contains(key)) {
            return false
        }

        return tag.getInt(key) > 0 || tag.getBoolean(key)
    }
}



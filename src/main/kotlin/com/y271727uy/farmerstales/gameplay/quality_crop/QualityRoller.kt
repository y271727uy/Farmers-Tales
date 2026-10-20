package com.y271727uy.farmerstales.gameplay.quality_crop

import com.y271727uy.farmerstales.config.Config
import net.minecraft.util.RandomSource

/** Rolls one quality tier using the common crop-quality configuration. */
object QualityRoller {
    fun roll(
        random: RandomSource,
        ironChance: Double = Config.ironCropQualityChance,
        goldChance: Double = Config.goldCropQualityChance,
        diamondChance: Double = Config.diamondCropQualityChance,
    ): Quality? {
        val result = random.nextDouble()
        val ironThreshold = ironChance
        val goldThreshold = ironThreshold + goldChance
        val diamondThreshold = goldThreshold + diamondChance

        return when {
            result < ironThreshold -> Quality.IRON
            result < goldThreshold -> Quality.GOLD
            result < diamondThreshold -> Quality.DIAMOND
            else -> null
        }
    }
}

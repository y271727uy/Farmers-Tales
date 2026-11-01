package com.y271727uy.farmerstales.integration.forestry.genome

import com.y271727uy.farmerstales.gameplay.gene_crop.CropTemperatureStatus
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import forestry.api.IForestryApi
import forestry.api.core.TemperatureType
import forestry.api.core.ToleranceType
import forestry.api.genetics.ClimateHelper
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack

/** Forestry API access stays behind the stand's optional-mod guard. */
object CropTemperature {
    @JvmStatic
    fun at(level: ServerLevel, pos: BlockPos, seed: ItemStack): CropTemperatureStatus {
        if (!GeneCropGenome.isGeneSeed(seed) || !GeneCropGenome.isIdentified(seed)) {
            return CropTemperatureStatus.SUITABLE
        }
        val actual = IForestryApi.INSTANCE.climateManager.getState(level, pos).temperature()
        return evaluate(actual, GeneCropGenome.temperatureTolerance(seed))
    }

    @JvmStatic
    fun evaluate(actual: TemperatureType, tolerance: ToleranceType): CropTemperatureStatus {
        val preferred = TemperatureType.NORMAL
        if (ClimateHelper.isWithinLimits(actual, preferred, tolerance)) return CropTemperatureStatus.SUITABLE
        val low = (preferred.ordinal - tolerance.down).coerceAtLeast(0)
        val high = (preferred.ordinal + tolerance.up).coerceAtMost(TemperatureType.VALUES.lastIndex)
        return when {
            actual.ordinal < low - 1 -> CropTemperatureStatus.TOO_COLD
            actual.ordinal < low -> CropTemperatureStatus.CHILLY
            actual.ordinal > high + 1 -> CropTemperatureStatus.TOO_HOT
            else -> CropTemperatureStatus.WARM
        }
    }
}

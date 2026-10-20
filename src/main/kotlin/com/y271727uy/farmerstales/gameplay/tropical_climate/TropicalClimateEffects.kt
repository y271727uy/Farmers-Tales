package com.y271727uy.farmerstales.gameplay.tropical_climate

import com.y271727uy.farmerstales.config.Config
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/** Resolves climate multipliers without altering vanilla weather or non-tropical biomes. */
object TropicalClimateEffects {
    fun treeWaterConsumptionChance(level: Level, pos: BlockPos, baseChance: Float): Float =
        (baseChance * when {
            TropicalClimateEvents.isPatternActiveAt(level, pos, TropicalClimatePattern.DROUGHT) ->
                Config.tropicalDroughtTreeWaterMultiplier.toFloat()
            TropicalClimateEvents.isPatternActiveAt(level, pos, TropicalClimatePattern.MONSOON) ->
                Config.tropicalMonsoonTreeWaterMultiplier.toFloat()
            else -> 1.0f
        }).coerceIn(0.0f, 1.0f)

    fun treeFertilizerConsumptionChance(level: Level, pos: BlockPos, baseChance: Float): Float =
        (baseChance * if (TropicalClimateEvents.isPatternActiveAt(level, pos, TropicalClimatePattern.MONSOON)) {
            Config.tropicalMonsoonTreeFertilizerMultiplier.toFloat()
        } else {
            1.0f
        }).coerceIn(0.0f, 1.0f)
}

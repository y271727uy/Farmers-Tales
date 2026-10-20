package com.y271727uy.farmerstales.integration.forestry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.forestry.genome.PestResistanceChromosome

/**
 * Forestry API types stay in this package. The common integration entry only
 * checks that the mod is present before touching them.
 */
object ForestryIntegration {
    fun init() {
        PestResistanceChromosome.alleles
        FTMod.LOGGER.debug("Forestry tree pest resistance registered")
    }
}

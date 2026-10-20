package com.y271727uy.farmerstales.integration.forestry;

import com.y271727uy.farmerstales.integration.forestry.genome.PestResistanceChromosome;
import forestry.api.genetics.alleles.IIntegerChromosome;

/**
 * Java-visible handle for the tree display mixins. The chromosome itself stays
 * in the Kotlin genome package, which compiles before these mixins.
 */
public final class PestResistanceDisplay {
    private PestResistanceDisplay() {
    }

    public static IIntegerChromosome chromosome() {
        return PestResistanceChromosome.INSTANCE.getChromosome();
    }
}

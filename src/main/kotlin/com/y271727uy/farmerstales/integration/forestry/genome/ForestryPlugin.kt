package com.y271727uy.farmerstales.integration.forestry.genome

import com.y271727uy.farmerstales.FTMod
import forestry.api.genetics.ForestrySpeciesTypes
import forestry.api.plugin.IForestryPlugin
import forestry.api.plugin.IGeneticRegistration
import net.minecraft.resources.ResourceLocation

/**
 * Loaded by Forestry's ServiceLoader. The karyotype edit has to happen here,
 * before species registration; a later Forge setup event is already too late.
 */
class ForestryPlugin : IForestryPlugin {
    override fun id(): ResourceLocation = ID

    override fun registerGenetics(genetics: IGeneticRegistration) {
        genetics.modifySpeciesType(ForestrySpeciesTypes.TREE) { type ->
            type.setKaryotype { karyotype ->
                karyotype.set(PestResistanceChromosome.chromosome, PestResistanceChromosome.alleles[3])
                    .addAlleles(PestResistanceChromosome.alleles)
            }
        }
    }

    companion object {
        val ID: ResourceLocation = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "forestry")
    }
}

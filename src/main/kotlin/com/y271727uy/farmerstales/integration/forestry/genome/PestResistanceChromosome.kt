package com.y271727uy.farmerstales.integration.forestry.genome

import com.y271727uy.farmerstales.FTMod
import forestry.api.genetics.alleles.ForestryAlleles
import forestry.api.genetics.alleles.IIntegerAllele
import forestry.api.genetics.alleles.IIntegerChromosome
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

/**
 * Forestry trees have no pest chromosome. The plugin appends this ladder to
 * the tree karyotype before it is built, so every tree species inherits it.
 * The first four alleles are dominant, matching Forestry's own ladders.
 */
object PestResistanceChromosome {
    const val CHROMOSOME_ID = "pest_resistance"

    val chromosome: IIntegerChromosome =
        ForestryAlleles.REGISTRY.intChromosome(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, CHROMOSOME_ID))

    val alleles: List<IIntegerAllele> = listOf(
        allele(1, dominant = true),
        allele(2, dominant = true),
        allele(3, dominant = true),
        allele(4, dominant = true),
        allele(5, dominant = false),
        allele(6, dominant = false),
        allele(7, dominant = false),
    )

    fun displayName(allele: IIntegerAllele): Component =
        chromosome.getDisplayName(allele)

    private fun allele(value: Int, dominant: Boolean): IIntegerAllele =
        ForestryAlleles.REGISTRY.intAllele(value, dominant)
}

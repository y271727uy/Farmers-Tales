package com.y271727uy.farmerstales.gameplay.gene_crop

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.forestry.genome.PestResistanceChromosome
import forestry.api.genetics.alleles.AllelePair
import forestry.api.genetics.alleles.BeeChromosomes
import forestry.api.genetics.alleles.ForestryAlleles
import forestry.api.genetics.alleles.IAllele
import forestry.api.genetics.alleles.IChromosome
import forestry.api.genetics.alleles.TreeChromosomes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack

/**
 * A gene crop is identified by the Forestry analyzer, but it is not a Forestry individual.
 * Identification stores both alleles of each trait, using Forestry's own allele ids.
 * The analyzer and tooltip ask those chromosomes for the display name.
 */
object GeneCropGenome {
    const val TAG = "Genome"
    const val GROWTH = "growth"
    const val YIELD = "yield"
    const val TEMPERATURE = "temperature"
    const val RESISTANCE = "resistance"
    private const val ACTIVE = "active"
    private const val INACTIVE = "inactive"
    private const val HONEY_PER_WORK = 100
    private const val TICKS_PER_WORK = 125

    @JvmField
    val SEEDS = ItemTags.create(ResourceLocation(FTMod.MODID, "gene_crop_seeds"))

    data class Trait(val label: Component, val active: Component, val inactive: Component)

    @JvmStatic
    fun isGeneSeed(stack: ItemStack): Boolean = !stack.isEmpty && stack.`is`(SEEDS)

    @JvmStatic
    fun isIdentified(stack: ItemStack): Boolean = genome(stack) != null

    @JvmStatic
    fun honeyPerWork(): Int = HONEY_PER_WORK

    @JvmStatic
    fun ticksPerWork(): Int = TICKS_PER_WORK

    @JvmStatic
    fun identify(stack: ItemStack, random: RandomSource) {
        if (!isGeneSeed(stack) || isIdentified(stack)) {
            return
        }
        val genome = CompoundTag()
        genome.put(GROWTH, rollPair(random, ForestryAlleles.DEFAULT_MATURATIONS))
        genome.put(YIELD, rollPair(random, ForestryAlleles.DEFAULT_YIELDS))
        genome.put(TEMPERATURE, rollPair(random, ForestryAlleles.DEFAULT_TEMPERATURE_TOLERANCES))
        genome.put(RESISTANCE, rollPair(random, PestResistanceChromosome.alleles))
        stack.getOrCreateTag().put(TAG, genome)
    }

    @JvmStatic
    fun traits(stack: ItemStack): List<Trait> {
        val genome = genome(stack) ?: return emptyList()
        val traits = ArrayList<Trait>(4)
        trait(genome, GROWTH, TreeChromosomes.MATURATION.getChromosomeTranslationKey(), TreeChromosomes.MATURATION)?.let(traits::add)
        trait(genome, YIELD, TreeChromosomes.YIELD.getChromosomeTranslationKey(), TreeChromosomes.YIELD)?.let(traits::add)
        trait(genome, TEMPERATURE, BeeChromosomes.TEMPERATURE_TOLERANCE.getChromosomeTranslationKey(), BeeChromosomes.TEMPERATURE_TOLERANCE)?.let(traits::add)
        trait(genome, RESISTANCE, "chromosome.farmerstales.pest_resistance", PestResistanceChromosome.chromosome)?.let(traits::add)
        return traits
    }

    @JvmStatic
    fun growthName(stack: ItemStack): Component? = traits(stack).firstOrNull()?.active

    @JvmStatic
    fun yieldName(stack: ItemStack): Component? = traits(stack).getOrNull(1)?.active

    @JvmStatic
    fun temperatureName(stack: ItemStack): Component? = traits(stack).getOrNull(2)?.active

    @JvmStatic
    fun resistanceName(stack: ItemStack): Component? = traits(stack).getOrNull(3)?.active

    private fun <A : IAllele> rollPair(random: RandomSource, alleles: List<A>): CompoundTag {
        val first = alleles[random.nextInt(alleles.size)]
        val second = alleles[random.nextInt(alleles.size)]
        val pair = AllelePair.create(first, second)
        val tag = CompoundTag()
        tag.putString(ACTIVE, pair.active().alleleId().toString())
        tag.putString(INACTIVE, pair.inactive().alleleId().toString())
        return tag
    }

    private fun genome(stack: ItemStack): CompoundTag? {
        val tag = stack.tag ?: return null
        if (!tag.contains(TAG, Tag.TAG_COMPOUND.toInt())) {
            return null
        }
        val genome = tag.getCompound(TAG)
        return if (
            genome.contains(GROWTH) ||
            genome.contains(YIELD) ||
            genome.contains(TEMPERATURE) ||
            genome.contains(RESISTANCE)
        ) genome else null
    }

    private fun trait(genome: CompoundTag, key: String, labelKey: String, chromosome: IChromosome<*>): Trait? {
        if (!genome.contains(key, Tag.TAG_COMPOUND.toInt())) {
            return null
        }
        val pair = genome.getCompound(key)
        val active = alleleName(pair, ACTIVE, chromosome) ?: return null
        val inactive = alleleName(pair, INACTIVE, chromosome) ?: return null
        return Trait(Component.translatable(labelKey), active, inactive)
    }

    private fun alleleName(pair: CompoundTag, key: String, chromosome: IChromosome<*>): Component? {
        if (!pair.contains(key, Tag.TAG_STRING.toInt())) {
            return null
        }
        val id = ResourceLocation.tryParse(pair.getString(key)) ?: return null
        val allele = ForestryAlleles.REGISTRY.getAllele(id) ?: return null
        return Component.translatable(translationKey(chromosome, allele))
    }

    /** Forestry's key is allele.<chromosome namespace>.<chromosome path>.<allele path>. */
    private fun translationKey(chromosome: IChromosome<*>, allele: IAllele): String {
        val chromosomeId = chromosome.id()
        val alleleId = allele.alleleId()
        val alleleName = if (alleleId.namespace == chromosomeId.namespace) {
            alleleId.path
        } else {
            "${alleleId.namespace}.${alleleId.path}"
        }
        return "allele.${chromosomeId.namespace}.${chromosomeId.path}.$alleleName"
    }
}

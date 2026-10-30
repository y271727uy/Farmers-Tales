package com.y271727uy.farmerstales.gameplay.gene_crop

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.forestry.genome.PestResistanceChromosome
import forestry.api.genetics.alleles.AllelePair
import forestry.api.genetics.alleles.BeeChromosomes
import forestry.api.genetics.alleles.ForestryAlleles
import forestry.api.genetics.alleles.IAllele
import forestry.api.genetics.alleles.IChromosome
import forestry.api.genetics.alleles.TreeChromosomes
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.ItemTags
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.BlockState

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

    /** Any crop whose seed is in [SEEDS]. The tag decides, not a hardcoded block. */
    @JvmStatic
    fun isGeneCrop(state: BlockState, level: LevelReader, pos: BlockPos): Boolean {
        val crop = state.block as? CropBlock ?: return false
        return isGeneSeed(crop.getCloneItemStack(level, pos, state))
    }

    @JvmStatic
    fun isIdentified(stack: ItemStack): Boolean = genome(stack) != null

    @JvmStatic
    fun copyGenome(from: ItemStack, to: ItemStack) {
        val genome = genome(from) ?: return
        to.getOrCreateTag().put(TAG, genome.copy())
    }

    @JvmStatic
    fun stored(stack: ItemStack): CompoundTag? = genome(stack)?.copy()

    /** Legacy no-op retained for callers from the capability migration. */
    @JvmStatic
    fun detachStoredTag(stack: ItemStack) {
    }

    @JvmStatic
    fun honeyPerWork(): Int = HONEY_PER_WORK

    @JvmStatic
    fun ticksPerWork(): Int = TICKS_PER_WORK

    /**
     * One generation, using Forestry's own Mendelian draw. Mutation is not part of this step.
     * A missing second parent self-pollinates and the offspring becomes homozygous.
     */
    @JvmStatic
    fun inherit(mother: ItemStack, father: ItemStack?, random: RandomSource): CompoundTag? {
        val maternal = genome(mother) ?: return null
        val child = CompoundTag()
        val paternal = father?.let(::genome)
        chromosome(GROWTH, TreeChromosomes.MATURATION, maternal, paternal, random)?.let { child.put(GROWTH, it) }
        chromosome(YIELD, TreeChromosomes.YIELD, maternal, paternal, random)?.let { child.put(YIELD, it) }
        chromosome(TEMPERATURE, BeeChromosomes.TEMPERATURE_TOLERANCE, maternal, paternal, random)?.let { child.put(TEMPERATURE, it) }
        chromosome(RESISTANCE, PestResistanceChromosome.chromosome, maternal, paternal, random)?.let { child.put(RESISTANCE, it) }
        return if (child.isEmpty) null else child
    }

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

    private fun <A : IAllele> chromosome(
        key: String,
        type: IChromosome<A>,
        maternal: CompoundTag,
        paternal: CompoundTag?,
        random: RandomSource,
    ): CompoundTag? {
        val mother = pair(maternal, key, type) ?: return null
        val inherited = if (paternal == null) {
            mother.inheritHaploid(random)
        } else {
            val father = pair(paternal, key, type) ?: return null
            mother.inheritOther(random, father)
        }
        return store(inherited)
    }

    private fun <A : IAllele> pair(genome: CompoundTag, key: String, type: IChromosome<A>): AllelePair<A>? {
        if (!genome.contains(key, Tag.TAG_COMPOUND.toInt())) {
            return null
        }
        val tag = genome.getCompound(key)
        val active = allele(tag, ACTIVE, type) ?: return null
        val inactive = allele(tag, INACTIVE, type) ?: return null
        return AllelePair(active, inactive)
    }

    private fun <A : IAllele> allele(tag: CompoundTag, key: String, type: IChromosome<A>): A? {
        if (!tag.contains(key, Tag.TAG_STRING.toInt())) {
            return null
        }
        val id = ResourceLocation.tryParse(tag.getString(key)) ?: return null
        val allele = ForestryAlleles.REGISTRY.getAllele(id) ?: return null
        @Suppress("UNCHECKED_CAST")
        return allele as? A
    }

    private fun store(pair: AllelePair<*>): CompoundTag {
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

package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.all.ModItems
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries

enum class BaitType {
    HERBIVOROUS,
    CARNIVOROUS,
}

data class MetabolismPlan(
    val maxHunger: Int,
    val hungerLossPerDay: Int,
    val initialWeight: Int,
    val maxWeight: Int,
    val overflowNutritionPerWeightGain: Int,
    val weightGainPerOverflow: Int,
    val mediumWeightThreshold: Int,
    val highWeightThreshold: Int,
    val starvationWeightLossPerDay: Int,
) {
    init {
        require(maxHunger > 0 && hungerLossPerDay in 1..maxHunger)
        require(initialWeight in 1..maxWeight)
        require(overflowNutritionPerWeightGain > 0 && weightGainPerOverflow > 0)
        require(mediumWeightThreshold in initialWeight..maxWeight)
        require(highWeightThreshold in (mediumWeightThreshold + 1)..maxWeight)
        require(starvationWeightLossPerDay > 0)
    }
}

data class AnimalHusbandryRule(
    val acceptedBaits: Set<BaitType>,
    val metabolism: MetabolismPlan,
    val breedingFoodNutrition: Int,
    val meatItemId: ResourceLocation?,
    val byproductItemId: ResourceLocation?,
)

/** Base diet table with optional per-animal DSL overrides. */
object AnimalHusbandryRules {
    private val defaultMetabolism = MetabolismPlan(
        maxHunger = 10,
        hungerLossPerDay = 5,
        initialWeight = 10,
        maxWeight = 30,
        overflowNutritionPerWeightGain = 10,
        weightGainPerOverflow = 5,
        mediumWeightThreshold = 10,
        highWeightThreshold = 20,
        starvationWeightLossPerDay = 5,
    )
    private val entityRules = linkedMapOf<ResourceLocation, AnimalHusbandryRule>()
    private val resolvedRules: Map<EntityType<*>, AnimalHusbandryRule> by lazy {
        buildMap {
            entityRules.forEach { (entityId, rule) ->
                ForgeRegistries.ENTITY_TYPES.getValue(entityId)?.let { put(it, rule) }
            }
        }
    }

    init {
        herbivore(
            "minecraft:cow",
            "minecraft:mooshroom",
            "minecraft:sheep",
            "minecraft:rabbit",
            "minecraft:camel",
            "minecraft:llama",
            "minecraft:horse",
            "minecraft:donkey",
        )
        omnivore("minecraft:chicken", "minecraft:pig")

        meat("minecraft:chicken", "minecraft:chicken")
        meat("minecraft:cow", "minecraft:beef")
        meat("minecraft:mooshroom", "minecraft:beef")
        meat("minecraft:sheep", "minecraft:mutton")
        meat("minecraft:pig", "minecraft:porkchop")
        meat("minecraft:rabbit", "minecraft:rabbit")

        custom("minecraft:chicken") {
            metabolism(maxHunger = 8, hungerLossPerDay = 4, initialWeight = 3, maxWeight = 12, overflowNutritionPerWeightGain = 6, weightGainPerOverflow = 3, mediumWeightThreshold = 4, highWeightThreshold = 8, starvationWeightLossPerDay = 3)
        }
        custom("minecraft:rabbit") {
            metabolism(maxHunger = 8, hungerLossPerDay = 4, initialWeight = 2, maxWeight = 8, overflowNutritionPerWeightGain = 5, weightGainPerOverflow = 2, mediumWeightThreshold = 3, highWeightThreshold = 5, starvationWeightLossPerDay = 2)
        }
        custom("minecraft:pig") {
            metabolism(maxHunger = 15, hungerLossPerDay = 5, initialWeight = 12, maxWeight = 36, overflowNutritionPerWeightGain = 15, weightGainPerOverflow = 6, mediumWeightThreshold = 12, highWeightThreshold = 24, starvationWeightLossPerDay = 6)
        }
        custom("minecraft:llama") {
            metabolism(maxHunger = 10, hungerLossPerDay = 5, initialWeight = 8, maxWeight = 24, overflowNutritionPerWeightGain = 10, weightGainPerOverflow = 4, mediumWeightThreshold = 8, highWeightThreshold = 16, starvationWeightLossPerDay = 4)
        }
        custom("minecraft:cow", "minecraft:mooshroom") {
            metabolism(maxHunger = 20, hungerLossPerDay = 5, initialWeight = 20, maxWeight = 60, overflowNutritionPerWeightGain = 20, weightGainPerOverflow = 10, mediumWeightThreshold = 20, highWeightThreshold = 40, starvationWeightLossPerDay = 10)
        }
        custom("minecraft:camel", "minecraft:horse", "minecraft:donkey") {
            metabolism(maxHunger = 18, hungerLossPerDay = 5, initialWeight = 18, maxWeight = 54, overflowNutritionPerWeightGain = 18, weightGainPerOverflow = 9, mediumWeightThreshold = 18, highWeightThreshold = 36, starvationWeightLossPerDay = 9)
        }
    }

    fun ruleFor(animal: Animal): AnimalHusbandryRule? = ruleFor(animal.type)

    fun ruleFor(entityType: EntityType<*>): AnimalHusbandryRule? = resolvedRules[entityType]

    fun isSupported(animal: Animal): Boolean = ruleFor(animal) != null

    fun acceptsBait(animal: Animal, stack: ItemStack): Boolean =
        baitType(stack)?.let { bait -> ruleFor(animal)?.acceptedBaits?.contains(bait) } == true

    fun metabolismFor(animal: Animal): MetabolismPlan? = ruleFor(animal)?.metabolism

    fun breedingFoodNutrition(animal: Animal): Int = ruleFor(animal)?.breedingFoodNutrition ?: 0

    fun meatItem(animal: Animal): Item? = ruleFor(animal)?.meatItemId?.let(ForgeRegistries.ITEMS::getValue)

    fun byproductItem(animal: Animal): Item? = ruleFor(animal)?.byproductItemId?.let(ForgeRegistries.ITEMS::getValue)

    /** A configured byproduct takes priority; primary meat makes weight rewards visible by default. */
    fun slaughterBonusItem(animal: Animal): Item? = byproductItem(animal) ?: meatItem(animal)

    fun baitType(stack: ItemStack): BaitType? = when {
        stack.`is`(ModItems.SPECIALIZED_HERBIVOROUS_BAIT.get()) -> BaitType.HERBIVOROUS
        stack.`is`(ModItems.SPECIALIZED_CARNIVOROUS_BAIT.get()) -> BaitType.CARNIVOROUS
        else -> null
    }

    private fun herbivore(vararg entityIds: String) = register(setOf(BaitType.HERBIVOROUS), *entityIds)

    private fun carnivore(vararg entityIds: String) = register(setOf(BaitType.CARNIVOROUS), *entityIds)

    private fun omnivore(vararg entityIds: String) = register(setOf(BaitType.HERBIVOROUS, BaitType.CARNIVOROUS), *entityIds)

    private fun meat(entityId: String, itemId: String?) = updateRule(entityId) { it.copy(meatItemId = itemId?.let(::parseItemId)) }

    private fun byproduct(entityId: String, itemId: String?) = updateRule(entityId) { it.copy(byproductItemId = itemId?.let(::parseItemId)) }

    private fun custom(vararg entityIds: String, block: AnimalRuleOverrideBuilder.() -> Unit) {
        entityIds.forEach { entityId ->
            updateRule(entityId) { existing -> AnimalRuleOverrideBuilder(existing).apply(block).build() }
        }
    }

    private fun register(acceptedBaits: Set<BaitType>, vararg entityIds: String) {
        entityIds.forEach { entityId ->
            val parsedId = ResourceLocation.tryParse(entityId)
                ?: throw IllegalArgumentException("Invalid animal entity ID: $entityId")
            require(parsedId !in entityRules) { "Duplicate animal rule: $entityId" }
            entityRules[parsedId] = AnimalHusbandryRule(
                acceptedBaits = acceptedBaits,
                metabolism = defaultMetabolism,
                breedingFoodNutrition = 5,
                meatItemId = null,
                byproductItemId = null,
            )
        }
    }

    private fun updateRule(entityId: String, transform: (AnimalHusbandryRule) -> AnimalHusbandryRule) {
        val parsedId = ResourceLocation.tryParse(entityId)
            ?: throw IllegalArgumentException("Invalid animal entity ID: $entityId")
        val existing = entityRules[parsedId]
            ?: throw IllegalArgumentException("Animal must be registered before defining its override: $entityId")
        entityRules[parsedId] = transform(existing)
    }

    private class AnimalRuleOverrideBuilder(private val base: AnimalHusbandryRule) {
        private var metabolism = base.metabolism
        private var breedingFoodNutrition = base.breedingFoodNutrition
        private var meatItemId = base.meatItemId
        private var byproductItemId = base.byproductItemId

        fun metabolism(
            maxHunger: Int,
            hungerLossPerDay: Int,
            initialWeight: Int,
            maxWeight: Int,
            overflowNutritionPerWeightGain: Int,
            weightGainPerOverflow: Int,
            mediumWeightThreshold: Int,
            highWeightThreshold: Int,
            starvationWeightLossPerDay: Int,
        ) {
            metabolism = MetabolismPlan(
                maxHunger,
                hungerLossPerDay,
                initialWeight,
                maxWeight,
                overflowNutritionPerWeightGain,
                weightGainPerOverflow,
                mediumWeightThreshold,
                highWeightThreshold,
                starvationWeightLossPerDay,
            )
        }

        fun nutritionPerBreedingFood(amount: Int) {
            require(amount > 0) { "Breeding food nutrition must be positive" }
            breedingFoodNutrition = amount
        }

        fun meat(itemId: String?) {
            meatItemId = itemId?.let(::parseItemId)
        }

        fun byproduct(itemId: String?) {
            byproductItemId = itemId?.let(::parseItemId)
        }

        fun build(): AnimalHusbandryRule = AnimalHusbandryRule(
            acceptedBaits = base.acceptedBaits,
            metabolism = metabolism,
            breedingFoodNutrition = breedingFoodNutrition,
            meatItemId = meatItemId,
            byproductItemId = byproductItemId,
        )
    }

    private fun parseItemId(itemId: String): ResourceLocation = ResourceLocation.tryParse(itemId)
        ?: throw IllegalArgumentException("Invalid item ID: $itemId")
}

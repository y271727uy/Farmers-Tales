package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.Animal

data class AnimalVitals(
    var hunger: Int,
    var affection: Int,
    var weight: Int,
    var overflowNutrition: Int,
    var lastSettledDay: Long,
)

/**
 * Lazy daily metabolism. Values are only persisted for domesticated adults;
 * elapsed days are resolved in constant time whenever an animal is processed.
 */
object AnimalMetabolism {
    private const val DATA_KEY = "metabolism"
    private const val HUNGER_KEY = "hunger"
    private const val AFFECTION_KEY = "affection"
    private const val WEIGHT_KEY = "weight"
    private const val OVERFLOW_NUTRITION_KEY = "overflowNutrition"
    private const val LAST_SETTLED_DAY_KEY = "lastSettledDay"
    private const val LAST_PETTED_DAY_KEY = "lastPettedDay"
    private const val AUTUMN_WEIGHT_MULTIPLIER = 1.2

    const val MAX_AFFECTION = 100
    const val PET_AFFECTION_GAIN = 10

    fun isManaged(animal: Animal): Boolean =
        AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.DOMESTICATED && !animal.isBaby

    fun inspect(animal: Animal, applyStarvationDamage: Boolean = true): AnimalVitals? = refresh(animal, applyStarvationDamage)

    fun isFullyFed(animal: Animal): Boolean {
        val plan = AnimalHusbandryRules.metabolismFor(animal) ?: return false
        return inspect(animal)?.hunger == plan.maxHunger
    }

    fun consumeHungerFromFullyFed(animal: Animal, amount: Int): Boolean {
        require(amount > 0) { "Hunger cost must be positive" }
        val plan = metabolismPlanFor(animal) ?: return false
        val vitals = refresh(animal, plan) ?: return false
        if (vitals.hunger != plan.maxHunger || amount > vitals.hunger) {
            return false
        }

        vitals.hunger -= amount
        write(animal, vitals)
        return true
    }

    fun spendAffection(animal: Animal, amount: Int): Boolean {
        require(amount >= 0) { "Affection cost cannot be negative" }
        val vitals = refresh(animal) ?: return false
        if (vitals.affection < amount) {
            return false
        }
        vitals.affection -= amount
        write(animal, vitals)
        return true
    }

    fun feed(animal: Animal, nutrition: Int): AnimalVitals? {
        require(nutrition >= 0) { "Nutrition cannot be negative" }
        val plan = metabolismPlanFor(animal) ?: return null
        val vitals = refresh(animal, plan) ?: return null
        val hungerRestored = minOf(nutrition, plan.maxHunger - vitals.hunger)
        vitals.hunger += hungerRestored

        val overflow = nutrition - hungerRestored
        if (overflow > 0 && vitals.weight < plan.maxWeight) {
            vitals.overflowNutrition += overflow
            val weightGained = (vitals.overflowNutrition / plan.overflowNutritionPerWeightGain * plan.weightGainPerOverflow * autumnWeightMultiplier(animal)).toInt()
            if (weightGained > 0) {
                vitals.weight = minOf(plan.maxWeight, vitals.weight + weightGained)
                vitals.overflowNutrition %= plan.overflowNutritionPerWeightGain
            }
            if (vitals.weight == plan.maxWeight) {
                vitals.overflowNutrition = 0
            }
        }

        write(animal, vitals)
        return vitals
    }

    /** Pets a domesticated animal once per Minecraft day. */
    fun pet(animal: Animal): Boolean {
        if (AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.DOMESTICATED) {
            return false
        }
        val plan = AnimalHusbandryRules.metabolismFor(animal) ?: return false
        val currentDay = animal.level().dayTime / 24_000L
        val vitals = if (animal.isBaby) {
            read(animal, plan) ?: AnimalVitals(
                hunger = plan.maxHunger,
                affection = 0,
                weight = plan.initialWeight,
                overflowNutrition = 0,
                lastSettledDay = currentDay,
            )
        } else {
            refresh(animal) ?: return false
        }
        val data = animal.persistentData.getCompound(DATA_KEY)
        if (data.contains(LAST_PETTED_DAY_KEY, Tag.TAG_LONG.toInt()) &&
            data.getLong(LAST_PETTED_DAY_KEY) == currentDay
        ) {
            return false
        }

        vitals.affection = (vitals.affection + PET_AFFECTION_GAIN).coerceAtMost(MAX_AFFECTION)
        write(animal, vitals)
        data.putLong(LAST_PETTED_DAY_KEY, currentDay)
        return true
    }

    fun refresh(animal: Animal, applyStarvationDamage: Boolean = true): AnimalVitals? {
        val plan = metabolismPlanFor(animal) ?: return null
        return refresh(animal, plan, applyStarvationDamage)
    }

    /** Resolves a scheduled daily update and reports whether it consumed hunger this day. */
    fun settleDaily(animal: Animal): Boolean {
        val plan = metabolismPlanFor(animal) ?: return false
        val result = refreshResult(animal, plan)
        return result.elapsedDays == 1L && result.hungerBefore > result.vitals.hunger
    }

    private fun refresh(animal: Animal, plan: MetabolismPlan, applyStarvationDamage: Boolean = true): AnimalVitals? {
        return refreshResult(animal, plan, applyStarvationDamage).vitals
    }

    private fun refreshResult(
        animal: Animal,
        plan: MetabolismPlan,
        applyStarvationDamage: Boolean = true,
    ): RefreshResult {
        val currentDay = animal.level().dayTime / 24_000L
        val vitals = read(animal, plan) ?: AnimalVitals(
            hunger = plan.maxHunger,
            affection = 0,
            weight = plan.initialWeight,
            overflowNutrition = 0,
            lastSettledDay = currentDay,
        )
        val elapsedDays = (currentDay - vitals.lastSettledDay).coerceAtLeast(0)
        val hungerBefore = vitals.hunger
        if (elapsedDays > 0) {
            settle(vitals, plan, elapsedDays)
            vitals.lastSettledDay = currentDay
        }
        write(animal, vitals)

        if (applyStarvationDamage && vitals.weight <= 0 && !animal.isRemoved) {
            animal.hurt(animal.damageSources().starve(), Float.MAX_VALUE)
        }
        return RefreshResult(vitals, elapsedDays, hungerBefore)
    }

    private fun settle(vitals: AnimalVitals, plan: MetabolismPlan, elapsedDays: Long) {
        var remainingDays = elapsedDays
        while (remainingDays > 0 && vitals.hunger > 0) {
            if (vitals.hunger == plan.maxHunger) {
                vitals.affection = (vitals.affection + 10).coerceAtMost(MAX_AFFECTION)
            }
            vitals.hunger = (vitals.hunger - plan.hungerLossPerDay).coerceAtLeast(0)
            remainingDays--
        }
        if (remainingDays > 0) {
            vitals.affection = (vitals.affection.toLong() - remainingDays * 10).coerceAtLeast(0).toInt()
            vitals.weight = (vitals.weight.toLong() - remainingDays * plan.starvationWeightLossPerDay).coerceAtLeast(0).toInt()
        }
    }

    /** Autumn pasture puts weight on faster. Hunger recovery is unchanged. */
    private fun autumnWeightMultiplier(animal: Animal): Double {
        val level = animal.level() as? ServerLevel ?: return 1.0
        if (!IntegrationManager.isSereneSeasonsLoaded()) return 1.0
        return if (SeasonSupport.currentSeasonWindow(level) == SeasonSupport.SeasonWindow.AUTUMN) {
            AUTUMN_WEIGHT_MULTIPLIER
        } else {
            1.0
        }
    }

    private fun metabolismPlanFor(animal: Animal): MetabolismPlan? =
        AnimalHusbandryRules.metabolismFor(animal)?.takeIf { isManaged(animal) }

    private fun read(animal: Animal, plan: MetabolismPlan): AnimalVitals? {
        val animalData = animal.persistentData
        if (!animalData.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            return null
        }
        val data = animalData.getCompound(DATA_KEY)
        return AnimalVitals(
            hunger = data.getInt(HUNGER_KEY).coerceIn(0, plan.maxHunger),
            affection = data.getInt(AFFECTION_KEY).coerceIn(0, MAX_AFFECTION),
            weight = data.getInt(WEIGHT_KEY).coerceIn(0, plan.maxWeight),
            overflowNutrition = data.getInt(OVERFLOW_NUTRITION_KEY).coerceIn(0, plan.overflowNutritionPerWeightGain - 1),
            lastSettledDay = data.getLong(LAST_SETTLED_DAY_KEY),
        )
    }

    private fun write(animal: Animal, vitals: AnimalVitals) {
        val animalData = animal.persistentData
        val data = if (animalData.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            animalData.getCompound(DATA_KEY)
        } else {
            CompoundTag().also { animalData.put(DATA_KEY, it) }
        }
        data.putInt(HUNGER_KEY, vitals.hunger)
        data.putInt(AFFECTION_KEY, vitals.affection)
        data.putInt(WEIGHT_KEY, vitals.weight)
        data.putInt(OVERFLOW_NUTRITION_KEY, vitals.overflowNutrition)
        data.putLong(LAST_SETTLED_DAY_KEY, vitals.lastSettledDay)
    }

    private data class RefreshResult(
        val vitals: AnimalVitals,
        val elapsedDays: Long,
        val hungerBefore: Int,
    )
}

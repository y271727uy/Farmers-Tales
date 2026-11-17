package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Sheep

/**
 * Statuses of a domesticated animal. Several may apply at once.
 * Declaration order is both display order and priority when the list is truncated.
 */
enum class AnimalCondition(val serializedName: String) {
    BABY("baby"),
    PANIC("panic"),
    HAPPY("happy"),
    DISEASED("diseased"),
    INFECTED("infected"),
    HUNGRY("hungry"),
    PRODUCING("producing"),
    IN_HEAT("in_heat");

    val bit: Int get() = 1 shl ordinal
    val translationKey: String get() = "tooltip.${FTMod.MODID}.animal_husbandry.condition.$serializedName"

    companion object {
        fun fromBits(bits: Int): List<AnimalCondition> = entries.filter { bits and it.bit != 0 }
    }
}

/** Resolves statuses on demand (Jade only); nothing here is stored or ticked. Server side only. */
object AnimalConditions {
    /** A chicken counts as producing when its next egg is due within this many ticks. */
    private const val EGG_DUE_TICKS = 60 * 20

    /** At most this many statuses are shown; lower-priority ones are dropped. */
    const val MAX_DISPLAYED = 3

    fun evaluate(
        animal: Animal,
        vitals: AnimalVitals? = AnimalMetabolism.inspect(animal),
        emotion: AnimalEmotionState? = AnimalEmotion.evaluate(animal, vitals),
    ): Set<AnimalCondition> {
        if (!AnimalEmotion.isApplicable(animal)) {
            return emptySet()
        }

        // Compatibility with the mood status:
        //   panic : disease, infection
        //   calm  : disease, infection, hunger, producing, in heat
        //   happy : infection, producing, in heat
        // Happy cannot carry disease or hunger because both cap the mood at calm (see AnimalEmotion).
        val mood = moodCondition(emotion)
        val panicking = mood == AnimalCondition.PANIC
        val result = sortedSetOf<AnimalCondition>()
        if (animal.isBaby) {
            result += AnimalCondition.BABY
        }
        mood?.let { result += it }
        if (isDiseased(animal) && mood != AnimalCondition.HAPPY) {
            result += AnimalCondition.DISEASED
        }
        if (isInfected(animal)) {
            result += AnimalCondition.INFECTED
        }
        // Juveniles have no metabolism, cannot breed and produce nothing.
        if (animal.isBaby) {
            return result
        }

        val plan = AnimalHusbandryRules.metabolismFor(animal)
        if (!panicking && vitals != null && plan != null && vitals.hunger * 2 < plan.maxHunger) {
            result += AnimalCondition.HUNGRY
        }
        if (!panicking && isReadyToProduce(animal)) {
            result += AnimalCondition.PRODUCING
        }
        if (!panicking && animal.isInLove) {
            result += AnimalCondition.IN_HEAT
        }
        return result
    }

    /** Mood statuses mirror the emotion: both distressed moods are panic, both positive moods are happy, calm adds none. */
    fun moodCondition(emotion: AnimalEmotionState?): AnimalCondition? = when (emotion) {
        AnimalEmotionState.TERRIFIED, AnimalEmotionState.PANICKED -> AnimalCondition.PANIC
        AnimalEmotionState.HAPPY, AnimalEmotionState.EXCITED -> AnimalCondition.HAPPY
        AnimalEmotionState.CALM, null -> null
    }

    /** Placeholder: infection, a standalone status compatible with every mood. */
    @Suppress("UNUSED_PARAMETER")
    fun isInfected(animal: Animal): Boolean = false

    /** Placeholder for the future disease system. A diseased animal can never be happy. */
    @Suppress("UNUSED_PARAMETER")
    fun isDiseased(animal: Animal): Boolean = false

    /** Mirrors the real production gates, so the status never promises output that would be refused. */
    fun isReadyToProduce(animal: Animal): Boolean {
        if (animal.isBaby || AnimalEmotion.isTerrified(animal)) {
            return false
        }
        return when (animal) {
            is Cow -> AnimalHarvestEvents.milkingCooldownRemaining(animal) == 0L && AnimalMetabolism.isFullyFed(animal)
            is Sheep -> animal.readyForShearing()
            is Chicken -> !animal.isChickenJockey && animal.eggTime <= EGG_DUE_TICKS
            else -> false
        }
    }
}

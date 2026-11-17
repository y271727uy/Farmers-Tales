package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.LivestockRaiderState
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Fox
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent
import net.minecraftforge.event.entity.living.LivingHurtEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap

/** Ordered from most distressed to most elated. */
enum class AnimalEmotionState(val serializedName: String) {
    TERRIFIED("terrified"),
    PANICKED("panicked"),
    CALM("calm"),
    HAPPY("happy"),
    EXCITED("excited");

    val translationKey: String get() = "tooltip.${FTMod.MODID}.animal_husbandry.emotion.$serializedName"

    companion object {
        fun fromSerializedName(value: String): AnimalEmotionState? = entries.firstOrNull { it.serializedName == value }
    }
}

/**
 * Emotion of domesticated animals, resolved on demand from current circumstances.
 * Only the time of the last injury is persisted; everything else is derived.
 *
 * There is deliberately no periodic settlement: emotion is evaluated only when something
 * asks (Jade, milking, love mode, egg laying). The one area scan involved is cached per
 * animal for [THREAT_CACHE_TICKS], so dense farms never rescan every tick.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalEmotion {
    private const val DATA_KEY = "farmerstales.animal_emotion"
    private const val LAST_HURT_KEY = "lastHurtTick"

    private const val TERRIFIED_HURT_TICKS = 10L * 20L
    private const val PANICKED_HURT_TICKS = 60L * 20L
    private const val TERRIFIED_THREAT_RADIUS = 6.0
    private const val PANICKED_THREAT_RADIUS = 16.0
    private const val EXCITED_AFFECTION = 80
    private const val HAPPY_AFFECTION = 50
    private const val THREAT_CACHE_TICKS = 20L
    private const val EGG_RECHECK_MIN_TICKS = 20
    private const val EGG_RECHECK_SPREAD_TICKS = 20

    /** Server-thread only. Weak keys let unloaded animals drop out on their own. */
    private val threatCache = WeakHashMap<Animal, ThreatSample>()

    fun isApplicable(animal: Animal): Boolean =
        AnimalHusbandryRules.isSupported(animal) &&
            AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.DOMESTICATED

    /** Returns null for animals that are not domesticated. Server side only. */
    fun evaluate(animal: Animal, vitals: AnimalVitals? = AnimalMetabolism.inspect(animal)): AnimalEmotionState? {
        if (!isApplicable(animal) || animal.level().isClientSide) {
            return null
        }
        val injury = injuryDistress(animal)
        val threatDistance = nearestThreatDistance(animal)
        val plan = AnimalHusbandryRules.metabolismFor(animal)

        if (injury == AnimalEmotionState.TERRIFIED || threatDistance <= TERRIFIED_THREAT_RADIUS) {
            return AnimalEmotionState.TERRIFIED
        }
        // Only danger causes distress. Hunger belongs to the calm mood and is shown as a status instead.
        if (injury == AnimalEmotionState.PANICKED || threatDistance <= PANICKED_THREAT_RADIUS) {
            return AnimalEmotionState.PANICKED
        }
        // Hunger and disease are carried by the calm mood: neither can coexist with a positive one.
        // (Hunger already blocks it through the HAPPY threshold below.) Infection does not cap the mood.
        if (AnimalConditions.isDiseased(animal)) {
            return AnimalEmotionState.CALM
        }
        if (animal.isInLove) {
            return AnimalEmotionState.EXCITED
        }
        if (vitals == null || plan == null) {
            return AnimalEmotionState.CALM
        }
        if (vitals.affection >= EXCITED_AFFECTION && vitals.hunger == plan.maxHunger &&
            AnimalMetabolism.wasPettedToday(animal)
        ) {
            return AnimalEmotionState.EXCITED
        }
        if (vitals.affection >= HAPPY_AFFECTION && vitals.hunger * 2 >= plan.maxHunger) {
            return AnimalEmotionState.HAPPY
        }
        return AnimalEmotionState.CALM
    }

    /**
     * Cheap check used by production and breeding gates: only the terrified triggers
     * are evaluated, without refreshing metabolism.
     */
    @JvmStatic
    fun isTerrified(animal: Animal): Boolean {
        if (animal.level().isClientSide || !isApplicable(animal)) {
            return false
        }
        return injuryDistress(animal) == AnimalEmotionState.TERRIFIED ||
            nearestThreatDistance(animal) <= TERRIFIED_THREAT_RADIUS
    }

    fun isRecentlyHurt(animal: Animal): Boolean = ticksSinceHurt(animal) <= PANICKED_HURT_TICKS

    /**
     * Fear caused by injuries: raid trauma or a recent hit, eased by an ongoing care session.
     * Null when injuries no longer affect the mood.
     */
    private fun injuryDistress(animal: Animal): AnimalEmotionState? {
        val ticksSinceHurt = ticksSinceHurt(animal)
        val distress = when {
            ticksSinceHurt <= TERRIFIED_HURT_TICKS || AnimalCare.isTraumatized(animal) -> AnimalEmotionState.TERRIFIED
            ticksSinceHurt <= PANICKED_HURT_TICKS -> AnimalEmotionState.PANICKED
            else -> return null
        }
        return AnimalCare.soothe(animal, distress)
    }

    /** Staggered delay before a held hen re-checks, so held hens never rescan every tick in lockstep. */
    @JvmStatic
    fun eggRecheckDelay(animal: Animal): Int = EGG_RECHECK_MIN_TICKS + animal.random.nextInt(EGG_RECHECK_SPREAD_TICKS)

    /** Holds the egg timer just before laying while the hen is terrified. */
    @JvmStatic
    fun shouldHoldEgg(animal: Animal, eggTime: Int): Boolean = eggTime <= 1 && isTerrified(animal)

    @JvmStatic
    fun sendRefusal(animal: Animal, player: Player, reason: String) {
        player.displayClientMessage(
            Component.translatable("message.${FTMod.MODID}.animal_emotion.refuse_$reason", animal.displayName),
            true,
        )
    }

    /** Catches pairs already in love mode that became terrified before the child spawned. */
    @JvmStatic
    @SubscribeEvent
    fun onBabyEntitySpawn(event: BabyEntitySpawnEvent) {
        val parentA = event.parentA as? Animal ?: return
        val parentB = event.parentB as? Animal
        val terrifiedParent = when {
            isTerrified(parentA) -> parentA
            parentB != null && isTerrified(parentB) -> parentB
            else -> return
        }
        event.isCanceled = true
        event.causedByPlayer?.let { sendRefusal(terrifiedParent, it, "breeding") }
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onAnimalHurt(event: LivingHurtEvent) {
        val animal = event.entity as? Animal ?: return
        if (animal.level().isClientSide || event.amount <= 0.0F || !isApplicable(animal)) {
            return
        }
        animal.persistentData.getCompound(DATA_KEY).also { data ->
            data.putLong(LAST_HURT_KEY, animal.level().gameTime)
            animal.persistentData.put(DATA_KEY, data)
        }
        // A raider bite leaves lasting trauma that only care (or time) removes.
        val attacker = event.source.entity as? LivingEntity
        if (attacker != null && LivestockRaiderState.isRaider(attacker)) {
            AnimalCare.markTrauma(animal)
        }
    }

    fun lastHurtTick(animal: Animal): Long? {
        // Check before getCompound: it allocates a fresh tag for animals that were never hurt.
        val root = animal.persistentData
        if (!root.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            return null
        }
        val data = root.getCompound(DATA_KEY)
        return if (data.contains(LAST_HURT_KEY)) data.getLong(LAST_HURT_KEY) else null
    }

    private fun ticksSinceHurt(animal: Animal): Long {
        val lastHurt = lastHurtTick(animal) ?: return Long.MAX_VALUE
        return (animal.level().gameTime - lastHurt).coerceAtLeast(0L)
    }

    private fun nearestThreatDistance(animal: Animal): Double {
        val now = animal.level().gameTime
        threatCache[animal]?.let { sample ->
            if (now - sample.tick in 0 until THREAT_CACHE_TICKS) {
                return sample.distance
            }
        }
        val level = animal.level()
        val box = animal.boundingBox.inflate(PANICKED_THREAT_RADIUS)
        var nearestSq = Double.MAX_VALUE
        val consider = { entity: LivingEntity ->
            if (entity.isAlive) {
                nearestSq = minOf(nearestSq, entity.distanceToSqr(animal))
            }
        }
        // Entity sections bucket entities by class, so querying the threat classes directly
        // never visits the livestock packed into the same box. Scan cost no longer grows with farm size.
        level.getEntitiesOfClass(Monster::class.java, box).forEach(consider)
        level.getEntitiesOfClass(Wolf::class.java, box, LivestockRaiderState::isRaider).forEach(consider)
        level.getEntitiesOfClass(Fox::class.java, box, LivestockRaiderState::isRaider).forEach(consider)

        val distance = if (nearestSq == Double.MAX_VALUE) Double.MAX_VALUE else Math.sqrt(nearestSq)
        threatCache[animal] = ThreatSample(now, distance)
        return distance
    }

    private data class ThreatSample(val tick: Long, val distance: Double)
}

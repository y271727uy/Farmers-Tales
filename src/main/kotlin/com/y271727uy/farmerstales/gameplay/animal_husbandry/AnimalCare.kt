package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Raid trauma and aftercare.
 *
 * An animal bitten by a raider stays terrified for [TRAUMA_TICKS] unless cared for. Feeding or petting an
 * animal that needs care starts one non-stacking care session of [CARE_TICKS]: it heals [HEAL_STEPS] health
 * spread over the session, and the injury-driven fear fades step by step (terrified -> panicked -> gone).
 *
 * Only the timestamps are persisted. The per-tick loop visits nothing but animals in an active session.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalCare {
    private const val DATA_KEY = "farmerstales.animal_care"
    private const val TRAUMA_UNTIL_KEY = "traumaUntil"
    private const val CARE_START_KEY = "careStart"
    private const val HEALS_GIVEN_KEY = "healsGiven"

    private const val TRAUMA_TICKS = 5L * 60L * 20L
    private const val CARE_TICKS = 30L * 20L
    /** Fear is unchanged for this long after care starts, then eases to panicked until the session ends. */
    private const val SOOTHE_TICKS = 10L * 20L
    private const val HEAL_STEPS = 4
    private const val HEAL_PER_STEP = 1.0F
    private const val HEAL_INTERVAL_TICKS = CARE_TICKS / HEAL_STEPS

    /** Server-thread only. Holds just the animals with a running session. */
    private val activeSessions: MutableSet<Animal> = Collections.newSetFromMap(IdentityHashMap())

    fun markTrauma(animal: Animal) {
        writableData(animal).putLong(TRAUMA_UNTIL_KEY, animal.level().gameTime + TRAUMA_TICKS)
    }

    fun isTraumatized(animal: Animal): Boolean {
        val data = data(animal) ?: return false
        return data.contains(TRAUMA_UNTIL_KEY, Tag.TAG_LONG.toInt()) &&
            data.getLong(TRAUMA_UNTIL_KEY) > animal.level().gameTime
    }

    fun needsCare(animal: Animal): Boolean =
        isTraumatized(animal) || animal.health < animal.maxHealth || AnimalEmotion.isRecentlyHurt(animal)

    /** Whether a session is running and was not invalidated by a later injury. */
    fun isCaring(animal: Animal): Boolean {
        val careStart = careStart(animal) ?: return false
        return animal.level().gameTime - careStart < CARE_TICKS && isUninterrupted(animal, careStart)
    }

    /** Called after a successful feed or pet. Returns true when a new session started. */
    fun tryStartCare(animal: Animal): Boolean {
        val level = animal.level() as? ServerLevel ?: return false
        if (!AnimalEmotion.isApplicable(animal) || !animal.isAlive || isCaring(animal) || !needsCare(animal)) {
            return false
        }
        writableData(animal).apply {
            putLong(CARE_START_KEY, level.gameTime)
            putInt(HEALS_GIVEN_KEY, 0)
        }
        activeSessions += animal
        sendCareParticles(level, animal, 6)
        return true
    }

    /**
     * Applies care to an injury-driven [distress] (TERRIFIED or PANICKED). Threat proximity is handled by the
     * caller and is never soothed: care cannot calm an animal while the predator is still there.
     */
    fun soothe(animal: Animal, distress: AnimalEmotionState): AnimalEmotionState? {
        val careStart = careStart(animal) ?: return distress
        if (!isUninterrupted(animal, careStart)) {
            return distress
        }
        val elapsed = animal.level().gameTime - careStart
        return when {
            elapsed < SOOTHE_TICKS -> distress
            elapsed < CARE_TICKS -> AnimalEmotionState.PANICKED
            else -> null
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END || activeSessions.isEmpty()) {
            return
        }
        val iterator = activeSessions.iterator()
        while (iterator.hasNext()) {
            val animal = iterator.next()
            val careStart = careStart(animal)
            if (animal.isRemoved || !animal.isAlive || careStart == null || !isUninterrupted(animal, careStart)) {
                iterator.remove()
                continue
            }
            val elapsed = animal.level().gameTime - careStart
            val data = writableData(animal)
            val dueHeals = (elapsed / HEAL_INTERVAL_TICKS).toInt().coerceAtMost(HEAL_STEPS)
            var healsGiven = data.getInt(HEALS_GIVEN_KEY)
            if (healsGiven < dueHeals) {
                while (healsGiven < dueHeals) {
                    animal.heal(HEAL_PER_STEP)
                    healsGiven++
                }
                data.putInt(HEALS_GIVEN_KEY, healsGiven)
                (animal.level() as? ServerLevel)?.let { sendCareParticles(it, animal, 3) }
            }
            if (elapsed >= CARE_TICKS) {
                data.remove(TRAUMA_UNTIL_KEY)
                iterator.remove()
            }
        }
    }

    /** Resumes a session interrupted by a chunk unload; healing catches up from the stored timestamps. */
    @JvmStatic
    @SubscribeEvent
    fun onAnimalJoinLevel(event: EntityJoinLevelEvent) {
        val animal = event.entity as? Animal ?: return
        if (event.level.isClientSide || data(animal) == null) {
            return
        }
        val careStart = careStart(animal) ?: return
        if (isUninterrupted(animal, careStart) && writableData(animal).getInt(HEALS_GIVEN_KEY) < HEAL_STEPS) {
            activeSessions += animal
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onAnimalLeaveLevel(event: EntityLeaveLevelEvent) {
        val animal = event.entity as? Animal ?: return
        activeSessions.remove(animal)
    }

    private fun isUninterrupted(animal: Animal, careStart: Long): Boolean {
        val lastHurt = AnimalEmotion.lastHurtTick(animal) ?: return true
        return careStart >= lastHurt
    }

    private fun careStart(animal: Animal): Long? {
        val data = data(animal) ?: return null
        return if (data.contains(CARE_START_KEY, Tag.TAG_LONG.toInt())) data.getLong(CARE_START_KEY) else null
    }

    private fun sendCareParticles(level: ServerLevel, animal: Animal, count: Int) {
        level.sendParticles(
            ParticleTypes.HAPPY_VILLAGER,
            animal.x, animal.y + animal.bbHeight * 0.75, animal.z,
            count, 0.35, 0.25, 0.35, 0.0,
        )
    }

    /** Read-only access that does not allocate a tag for animals that were never involved. */
    private fun data(animal: Animal): CompoundTag? {
        val root = animal.persistentData
        return if (root.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) root.getCompound(DATA_KEY) else null
    }

    private fun writableData(animal: Animal): CompoundTag {
        val root = animal.persistentData
        return if (root.contains(DATA_KEY, Tag.TAG_COMPOUND.toInt())) {
            root.getCompound(DATA_KEY)
        } else {
            CompoundTag().also { root.put(DATA_KEY, it) }
        }
    }
}

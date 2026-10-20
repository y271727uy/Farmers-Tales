package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.event.entity.EntityLeaveLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import java.util.IdentityHashMap
import java.util.PriorityQueue
import java.util.WeakHashMap

/** Spreads daily metabolism across ticks instead of updating every animal at once. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalMetabolismScheduler {
    private val animalsByLevel = WeakHashMap<ServerLevel, MutableSet<Animal>>()
    private val lastDayByLevel = WeakHashMap<ServerLevel, Long>()
    private val pending = ArrayDeque<Animal>()
    private val queued = java.util.Collections.newSetFromMap(IdentityHashMap<Animal, Boolean>())
    private val dailySettlementPending = IdentityHashMap<Animal, Boolean>()
    private val maturationDueByAnimal = WeakHashMap<Animal, Long>()
    private val pendingMaturities = PriorityQueue<MaturityEntry>(compareBy(MaturityEntry::dueAt))

    @JvmStatic
    @SubscribeEvent
    fun onAnimalJoinLevel(event: EntityJoinLevelEvent) {
        val animal = event.entity as? Animal ?: return
        val level = event.level as? ServerLevel ?: return
        if (AnimalHusbandryRules.isSupported(animal) &&
            AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.DOMESTICATED
        ) {
            track(animal)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onAnimalLeaveLevel(event: EntityLeaveLevelEvent) {
        val animal = event.entity as? Animal ?: return
        val level = event.level as? ServerLevel ?: return
        animalsByLevel[level]?.remove(animal)
        queued.remove(animal)
        dailySettlementPending.remove(animal)
        maturationDueByAnimal.remove(animal)
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.END) {
            return
        }
        promoteMatureAnimals()
        event.server.allLevels.forEach(::enqueueDailySettlements)
        repeat(Config.metabolismSettlementsPerTick) {
            if (pending.isEmpty()) {
                return
            }
            val animal = pending.removeFirst()
            queued.remove(animal)
            val isDailySettlement = dailySettlementPending.remove(animal) == true
            if (!animal.isRemoved) {
                val hungerDecreased = if (isDailySettlement) {
                    AnimalMetabolism.settleDaily(animal)
                } else {
                    AnimalMetabolism.refresh(animal)
                    false
                }
                if (hungerDecreased) {
                    AnimalDungPlacement.tryPlaceAtFeet(animal)
                }
            }
        }
    }

    fun track(animal: Animal) {
        val level = animal.level() as? ServerLevel ?: return
        if (AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.DOMESTICATED) {
            return
        }
        if (animal.isBaby) {
            scheduleMaturity(animal, level)
            return
        }
        maturationDueByAnimal.remove(animal)
        animalsByLevel.getOrPut(level) { java.util.Collections.newSetFromMap(IdentityHashMap()) }.add(animal)
        enqueue(animal)
    }

    /** Called after vanilla has applied a baby's normal food growth bonus. */
    fun rescheduleMaturityAfterGrowthFood(animal: Animal) {
        val level = animal.level() as? ServerLevel ?: return
        level.server.execute { track(animal) }
    }

    private fun enqueueDailySettlements(level: ServerLevel) {
        val currentDay = level.dayTime / 24_000L
        val previousDay = lastDayByLevel.put(level, currentDay)
        if (previousDay == null || currentDay <= previousDay) {
            return
        }
        animalsByLevel[level]?.forEach { animal -> enqueue(animal, isDailySettlement = true) }
    }

    private fun enqueue(animal: Animal, isDailySettlement: Boolean = false) {
        if (queued.add(animal)) {
            pending += animal
            dailySettlementPending[animal] = isDailySettlement
        } else if (isDailySettlement) {
            dailySettlementPending[animal] = true
        }
    }

    private fun scheduleMaturity(animal: Animal, level: ServerLevel) {
        val dueAt = level.gameTime + -animal.age.toLong()
        maturationDueByAnimal[animal] = dueAt
        pendingMaturities += MaturityEntry(dueAt, WeakReference(level), WeakReference(animal))
    }

    private fun promoteMatureAnimals() {
        while (true) {
            val entry = pendingMaturities.peek() ?: return
            val level = entry.level.get()
            if (level == null) {
                pendingMaturities.remove()
                continue
            }
            if (level.gameTime < entry.dueAt) {
                return
            }
            pendingMaturities.remove()

            val animal = entry.animal.get() ?: continue
            if (maturationDueByAnimal[animal] != entry.dueAt) {
                continue
            }
            maturationDueByAnimal.remove(animal)
            if (animal.isRemoved || animal.level() !== level ||
                AnimalHusbandryState.status(animal) != AnimalHusbandryStatus.DOMESTICATED
            ) {
                continue
            }
            track(animal)
        }
    }

    private data class MaturityEntry(
        val dueAt: Long,
        val level: WeakReference<ServerLevel>,
        val animal: WeakReference<Animal>,
    )
}

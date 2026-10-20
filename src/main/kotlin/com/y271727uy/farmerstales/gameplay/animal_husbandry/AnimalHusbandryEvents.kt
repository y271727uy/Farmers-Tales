package com.y271727uy.farmerstales.gameplay.animal_husbandry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent
import net.minecraftforge.event.entity.living.MobSpawnEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object AnimalHusbandryEvents {
    private val naturalSpawnTypes = setOf(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION)

    @JvmStatic
    @SubscribeEvent
    fun onFinalizeSpawn(event: MobSpawnEvent.FinalizeSpawn) {
        val animal = event.entity as? Animal ?: return
        if (event.spawnType in naturalSpawnTypes && AnimalHusbandryRules.isSupported(animal)) {
            AnimalHusbandryState.markWild(animal)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onBabyEntitySpawn(event: BabyEntitySpawnEvent) {
        val child = event.child as? Animal ?: return
        if (!AnimalHusbandryRules.isSupported(child)) {
            return
        }

        val parentA = event.parentA as? Animal
        val parentB = event.parentB as? Animal
        applyChildStatus(child, parentA, parentB)
        if (AnimalHusbandryState.status(child) == AnimalHusbandryStatus.DOMESTICATED) {
            parentA?.loveCause?.let { player ->
                awardHusbandryAdvancement(player, "animal_husbandry/breed_domesticated_animals", child)
            }
        }
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPigBabyEntitySpawn(event: BabyEntitySpawnEvent) {
        val firstPiglet = event.child ?: return
        if (firstPiglet.type != EntityType.PIG) {
            return
        }
        val level = firstPiglet.level() as? ServerLevel ?: return
        val parentA = event.parentA as? Animal ?: return
        val parentB = event.parentB as? Animal

        if (parentA.random.nextDouble() >= Config.pigExtraBabyChance) {
            return
        }

        val piglet = EntityType.PIG.create(level) ?: return
        piglet.setBaby(true)
        piglet.moveTo(
            firstPiglet.x + (parentA.random.nextDouble() - 0.5),
            firstPiglet.y,
            firstPiglet.z + (parentA.random.nextDouble() - 0.5),
            parentA.random.nextFloat() * 360.0F,
            0.0F,
        )
        applyChildStatus(piglet, parentA, parentB)
        level.addFreshEntity(piglet)
    }

    private fun awardHusbandryAdvancement(player: ServerPlayer, id: String, animal: Animal) {
        val criterion = ForgeRegistries.ENTITY_TYPES.getKey(animal.type)?.path ?: return
        FarmersTalesAdvancements.award(player, id, criterion)
    }

    private fun applyChildStatus(child: Animal, parentA: Animal?, parentB: Animal?) {
        val bothParentsDomesticated = parentA != null && parentB != null &&
            AnimalHusbandryState.status(parentA) == AnimalHusbandryStatus.DOMESTICATED &&
            AnimalHusbandryState.status(parentB) == AnimalHusbandryStatus.DOMESTICATED

        if (bothParentsDomesticated) {
            AnimalHusbandryState.markDomesticated(child)
        } else {
            AnimalHusbandryState.markWild(child)
        }
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onAnimalInteractSpecific(event: PlayerInteractEvent.EntityInteractSpecific) {
        handleAnimalInteract(event, event.target as? Animal ?: return)
    }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onAnimalInteract(event: PlayerInteractEvent.EntityInteract) {
        handleAnimalInteract(event, event.target as? Animal ?: return)
    }

    private fun handleAnimalInteract(event: PlayerInteractEvent, animal: Animal) {
        if (event.level.isClientSide || event.hand != InteractionHand.MAIN_HAND) {
            return
        }

        if (AnimalHusbandryRules.isSupported(animal) &&
            AnimalHusbandryState.status(animal) == AnimalHusbandryStatus.UNMANAGED
        ) {
            AnimalHusbandryState.markWild(animal)
        }

        val heldItem = event.itemStack
        when (AnimalHusbandryState.status(animal)) {
            AnimalHusbandryStatus.WILD -> {
                if (AnimalHusbandryRules.acceptsBait(animal, heldItem)) {
                    AnimalHusbandryState.markDomesticated(animal)
                    awardHusbandryAdvancement(
                        event.entity as ServerPlayer,
                        "animal_husbandry/domesticate_animal",
                        animal,
                    )
                    AnimalMetabolismScheduler.track(animal)
                    if (!event.entity.abilities.instabuild) {
                        heldItem.shrink(1)
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS)
                    event.isCanceled = true
                    return
                }

                return
            }
            AnimalHusbandryStatus.DOMESTICATED -> {
                if (event.itemStack.isEmpty && AnimalMetabolism.pet(animal)) {
                    val level = animal.level() as ServerLevel
                    level.sendParticles(ParticleTypes.HEART, animal.x, animal.y + animal.bbHeight, animal.z, 5, 0.35, 0.2, 0.35, 0.0)
                    level.playSound(null, animal.blockPosition(), SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.5F, 1.5F)
                    event.setCancellationResult(InteractionResult.SUCCESS)
                    event.isCanceled = true
                    return
                }
                if (animal.isBaby && AnimalHusbandryRules.isSupported(animal) && animal.isFood(heldItem)) {
                    AnimalMetabolismScheduler.rescheduleMaturityAfterGrowthFood(animal)
                    return
                }
                if (AnimalHusbandryRules.isSupported(animal) && animal.isFood(heldItem)) {
                    val wasFullyFed = AnimalMetabolism.isFullyFed(animal)
                    AnimalMetabolism.feed(animal, AnimalHusbandryRules.breedingFoodNutrition(animal))
                    if (!event.entity.abilities.instabuild) {
                        heldItem.shrink(1)
                    }
                    // A fully fed animal has a limited chance to enter love mode after another feeding.
                    if (wasFullyFed && animal.canFallInLove() &&
                        animal.random.nextDouble() < Config.animalBreedingLoveChance
                    ) {
                        animal.setInLove(event.entity)
                    }
                    event.setCancellationResult(InteractionResult.SUCCESS)
                    event.isCanceled = true
                }
            }
            AnimalHusbandryStatus.UNMANAGED -> Unit
        }
    }
}

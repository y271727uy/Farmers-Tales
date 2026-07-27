package com.y271727uy.farmerstales.entity.ai

import net.minecraft.world.entity.ai.goal.BreedGoal
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal
import net.minecraft.world.entity.ai.goal.FloatGoal
import net.minecraft.world.entity.ai.goal.FollowParentGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.PanicGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.crafting.Ingredient
import java.util.function.Predicate

/** Common goal stack for breedable passive animals. */
data class PassiveAnimalAiDefinition(
    val panicSpeed: Double,
    val breedingSpeed: Double,
    val temptationSpeed: Double,
    val followParentSpeed: Double,
    val strollSpeed: Double,
    val playerLookRange: Float,
    val movingPlayerAvoidance: MovingPlayerAvoidanceDefinition?,
    val entityAvoidances: List<EntityAvoidanceDefinition>,
) {
    fun install(animal: Animal, breedingIngredient: Ingredient?) {
        animal.goalSelector.addGoal(0, FloatGoal(animal))
        entityAvoidances.forEach { avoidance ->
            installAvoidance(animal, avoidance)
        }
        movingPlayerAvoidance?.let { avoidance ->
            animal.goalSelector.addGoal(
                1,
                ConditionalAvoidEntityGoal(
                    animal,
                    avoidance.enabled,
                    Player::class.java,
                    { player -> player.deltaMovement.horizontalDistanceSqr() > avoidance.motionThresholdSquared },
                    avoidance.range,
                    avoidance.walkSpeed,
                    avoidance.sprintSpeed,
                    Predicate<LivingEntity> { player ->
                        net.minecraft.world.entity.EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(player)
                    },
                ),
            )
        }
        animal.goalSelector.addGoal(2, PanicGoal(animal, panicSpeed))
        animal.goalSelector.addGoal(3, BreedGoal(animal, breedingSpeed))
        breedingIngredient?.let { ingredient ->
            animal.goalSelector.addGoal(4, TemptGoal(animal, temptationSpeed, ingredient, false))
        }
        animal.goalSelector.addGoal(5, FollowParentGoal(animal, followParentSpeed))
        animal.goalSelector.addGoal(6, WaterAvoidingRandomStrollGoal(animal, strollSpeed))
        animal.goalSelector.addGoal(7, LookAtPlayerGoal(animal, Player::class.java, playerLookRange))
        animal.goalSelector.addGoal(8, RandomLookAroundGoal(animal))
    }

    @Suppress("UNCHECKED_CAST")
    private fun installAvoidance(animal: Animal, avoidance: EntityAvoidanceDefinition) {
        animal.goalSelector.addGoal(
            1,
            ConditionalAvoidEntityGoal(
                animal = animal,
                enabled = avoidance.enabled,
                entityType = avoidance.entityType as Class<LivingEntity>,
                range = avoidance.range,
                walkSpeed = avoidance.walkSpeed,
                sprintSpeed = avoidance.sprintSpeed,
            ),
        )
    }
}

data class MovingPlayerAvoidanceDefinition(
    val range: Float,
    val walkSpeed: Double,
    val sprintSpeed: Double,
    val motionThresholdSquared: Double,
    val enabled: (Animal) -> Boolean,
)

data class EntityAvoidanceDefinition(
    val entityType: Class<out LivingEntity>,
    val range: Float,
    val walkSpeed: Double,
    val sprintSpeed: Double,
    val enabled: (Animal) -> Boolean,
)

private class ConditionalAvoidEntityGoal<T : LivingEntity>(
    private val animal: Animal,
    private val enabled: (Animal) -> Boolean,
    entityType: Class<T>,
    avoidPredicate: Predicate<LivingEntity> = Predicate { true },
    range: Float,
    walkSpeed: Double,
    sprintSpeed: Double,
    targetPredicate: Predicate<LivingEntity> = Predicate { true },
) : AvoidEntityGoal<T>(animal, entityType, avoidPredicate, range, walkSpeed, sprintSpeed, targetPredicate) {
    override fun canUse(): Boolean = enabled(animal) && super.canUse()

    override fun canContinueToUse(): Boolean = enabled(animal) && super.canContinueToUse()
}

class MobAiBuilder {
    private var passiveAnimalDefinition: PassiveAnimalAiDefinition? = null

    fun passiveAnimal(configure: PassiveAnimalAiBuilder.() -> Unit = {}) {
        passiveAnimalDefinition = PassiveAnimalAiBuilder().apply(configure).build()
    }

    internal fun build(): PassiveAnimalAiDefinition? = passiveAnimalDefinition
}

class PassiveAnimalAiBuilder {
    var panicSpeed: Double = 1.25
    var breedingSpeed: Double = 1.0
    var temptationSpeed: Double = 1.1
    var followParentSpeed: Double = 1.1
    var strollSpeed: Double = 1.0
    var playerLookRange: Float = 6.0F
    private var movingPlayerAvoidance: MovingPlayerAvoidanceDefinition? = null
    private val entityAvoidances = mutableListOf<EntityAvoidanceDefinition>()

    /** Flee players that are moving; a still player can safely offer breeding food. */
    fun avoidMovingPlayers(configure: MovingPlayerAvoidanceBuilder.() -> Unit = {}) {
        movingPlayerAvoidance = MovingPlayerAvoidanceBuilder().apply(configure).build()
    }

    fun <T : LivingEntity> avoid(entityType: Class<T>, configure: EntityAvoidanceBuilder.() -> Unit = {}) {
        entityAvoidances += EntityAvoidanceBuilder().apply(configure).build(entityType)
    }

    internal fun build(): PassiveAnimalAiDefinition = PassiveAnimalAiDefinition(
        panicSpeed = panicSpeed,
        breedingSpeed = breedingSpeed,
        temptationSpeed = temptationSpeed,
        followParentSpeed = followParentSpeed,
        strollSpeed = strollSpeed,
        playerLookRange = playerLookRange,
        movingPlayerAvoidance = movingPlayerAvoidance,
        entityAvoidances = entityAvoidances.toList(),
    )
}

class EntityAvoidanceBuilder {
    var range: Float = 8.0F
    var walkSpeed: Double = 1.0
    var sprintSpeed: Double = 1.25
    private var enabled: (Animal) -> Boolean = { true }

    fun untamedOnly() {
        enabled = { animal -> animal !is net.minecraft.world.entity.TamableAnimal || !animal.isTame }
    }

    internal fun build(entityType: Class<out LivingEntity>): EntityAvoidanceDefinition {
        require(range > 0.0F) { "Entity avoidance range must be positive" }
        require(walkSpeed > 0.0 && sprintSpeed > 0.0) { "Entity avoidance speeds must be positive" }
        return EntityAvoidanceDefinition(entityType, range, walkSpeed, sprintSpeed, enabled)
    }
}

class MovingPlayerAvoidanceBuilder {
    var range: Float = 8.0F
    var walkSpeed: Double = 1.0
    var sprintSpeed: Double = 1.25
    var motionThreshold: Double = 0.01
    private var enabled: (Animal) -> Boolean = { true }

    fun untamedOnly() {
        enabled = { animal -> animal !is net.minecraft.world.entity.TamableAnimal || !animal.isTame }
    }

    internal fun build(): MovingPlayerAvoidanceDefinition {
        require(range > 0.0F) { "Player avoidance range must be positive" }
        require(walkSpeed > 0.0 && sprintSpeed > 0.0) { "Player avoidance speeds must be positive" }
        require(motionThreshold >= 0.0) { "Player motion threshold cannot be negative" }
        return MovingPlayerAvoidanceDefinition(range, walkSpeed, sprintSpeed, motionThreshold * motionThreshold, enabled)
    }
}

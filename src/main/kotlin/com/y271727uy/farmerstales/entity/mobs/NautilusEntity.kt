package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.FluidTags
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.MoverType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.BreedGoal
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.entity.ai.goal.TemptGoal
import net.minecraft.world.entity.ai.navigation.PathNavigation
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.pathfinder.BlockPathTypes
import net.minecraft.world.phys.Vec3

/**
 * A tameable aquatic mount. While a player rides it underwater their air is continually restored,
 * which recreates the useful part of the modern "Breath of the Nautilus" feature on 1.20.1.
 *
 * Swimming mirrors vanilla [net.minecraft.world.entity.animal.Dolphin] (water navigation, smooth swim
 * move/look controls, speed-driven travel and zero water malus) while staying a [TamableAnimal] so taming,
 * breeding and the saddle mount keep working. Dolphin-only behaviour such as surfacing for air is omitted.
 */
open class NautilusEntity(type: EntityType<out NautilusEntity>, level: Level) : TamableAnimal(type, level) {
    init {
        // Animals default to an 8.0 water malus, which makes random swim targets reject every water block.
        setPathfindingMalus(BlockPathTypes.WATER, 0.0F)
        moveControl = SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true)
        lookControl = SmoothSwimmingLookControl(this, 10)
    }

    override fun createNavigation(level: Level): PathNavigation = WaterBoundPathNavigation(this, level)

    /** Only the tameable nautilus is lured by and breeds with pufferfish. */
    protected open fun hasBreedingGoals(): Boolean = true

    val isSaddled: Boolean
        get() = entityData.get(SADDLED)

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(SADDLED, false)
    }

    override fun registerGoals() {
        if (hasBreedingGoals()) {
            goalSelector.addGoal(1, BreedGoal(this, 1.0))
            goalSelector.addGoal(2, TemptGoal(this, 1.2, Ingredient.of(Items.PUFFERFISH), false))
        }
        goalSelector.addGoal(4, RandomSwimmingGoal(this, 1.0, 10))
        goalSelector.addGoal(5, LookAtPlayerGoal(this, Player::class.java, 6.0F))
        goalSelector.addGoal(6, RandomLookAroundGoal(this))
    }

    override fun getMaxHeadXRot(): Int = 1

    override fun getMaxHeadYRot(): Int = 1

    override fun checkSpawnObstruction(level: LevelReader): Boolean = level.isUnobstructed(this)

    /** Animal prefers bright blocks, which drags random swim targets to the surface; dolphins use 0. */
    override fun getWalkTargetValue(pos: BlockPos, level: LevelReader): Float = 0.0F

    override fun isFood(stack: ItemStack): Boolean = ModEntities.NAUTILUS.isBreedingFood(stack)

    /** Like vanilla wolves, a tamed parent passes its owner on to the baby. */
    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? =
        ModEntities.NAUTILUS.type.get().create(level)?.also { baby ->
            ownerUUID?.let { owner ->
                baby.ownerUUID = owner
                baby.setTame(true)
            }
        }

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (!isTame && stack.`is`(Items.PUFFERFISH)) {
            if (!level().isClientSide) {
                usePlayerItem(player, hand, stack)
                if (random.nextInt(3) == 0) {
                    tame(player)
                    level().broadcastEntityEvent(this, 7)
                } else {
                    level().broadcastEntityEvent(this, 6)
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        if (isTame && isOwnedBy(player) && !isSaddled && stack.`is`(Items.SADDLE)) {
            if (!level().isClientSide) {
                entityData.set(SADDLED, true)
                if (!player.abilities.instabuild) stack.shrink(1)
                playSound(SoundEvents.HORSE_SADDLE, 0.8F, 1.0F)
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        if (isTame && isOwnedBy(player) && isSaddled && !isBaby && !isVehicle) {
            if (!level().isClientSide) player.startRiding(this)
            return InteractionResult.sidedSuccess(level().isClientSide)
        }
        return super.mobInteract(player, hand)
    }

    override fun tick() {
        super.tick()
        val rider = controllingPassenger
        if (rider is Player && isInWater) rider.airSupply = rider.maxAirSupply
        if (!isInWater) airSupply = maxAirSupply
    }

    override fun travel(travelVector: Vec3) {
        val rider = controllingPassenger as? LivingEntity
        if (isInWaterOrBubble) {
            if (rider != null && isSaddled) {
                yRot = rider.yRot
                yRotO = yRot
                xRot = rider.xRot * 0.5F
                setRot(yRot, xRot)
                yBodyRot = yRot
                yHeadRot = yRot

                val forward = rider.zza.toDouble()
                val strafe = rider.xxa.toDouble() * 0.55
                val facing = Vec3.directionFromRotation(xRot, yRot)
                val sideways = Vec3.directionFromRotation(0.0F, yRot + 90.0F)
                val movement = facing.scale(forward * RIDING_SPEED).add(sideways.scale(strafe * RIDING_SPEED))
                setDeltaMovement(movement.x, movement.y + facing.y * RIDING_SPEED, movement.z)
            } else if (isEffectiveAi) {
                // Same as Dolphin: SmoothSwimmingMoveControl sets the per-tick speed for the current path.
                moveRelative(speed, travelVector)
            }
            move(MoverType.SELF, deltaMovement)
            setDeltaMovement(deltaMovement.scale(WATER_DRAG))
            if (rider == null && target == null) {
                setDeltaMovement(deltaMovement.add(0.0, PASSIVE_DESCENT, 0.0))
            }
            return
        }
        super.travel(travelVector)
    }

    override fun canAddPassenger(passenger: Entity): Boolean = passengers.isEmpty()

    override fun canBreatheUnderwater(): Boolean = true

    /** Water creatures are not carried to the surface by the fluid current. */
    override fun isPushedByFluid(): Boolean = false

    override fun getAmbientSound(): SoundEvent = SoundEvents.TURTLE_AMBIENT_LAND

    override fun getHurtSound(damageSource: net.minecraft.world.damagesource.DamageSource): SoundEvent = SoundEvents.TURTLE_HURT

    override fun getDeathSound(): SoundEvent = SoundEvents.TURTLE_DEATH

    override fun playStepSound(pos: BlockPos, block: BlockState) = Unit

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putBoolean("Saddled", isSaddled)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        entityData.set(SADDLED, tag.getBoolean("Saddled"))
        // Saved attribute bases survive updates; migrate nautiluses saved with the old 0.22 travel speed.
        getAttribute(Attributes.MOVEMENT_SPEED)?.let { speed ->
            if (speed.baseValue < LEGACY_SPEED_THRESHOLD) speed.baseValue = MOVEMENT_SPEED
        }
    }

    companion object {
        private const val RIDING_SPEED = 0.18
        private const val WATER_DRAG = 0.9
        private const val MOVEMENT_SPEED = 1.0
        private const val LEGACY_SPEED_THRESHOLD = 0.5
        private const val PASSIVE_DESCENT = -0.005
        private val SADDLED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(NautilusEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder = TamableAnimal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            // Scaled by SmoothSwimmingMoveControl (x0.02 in water): ~4 blocks/s cruising, slightly below a dolphin.
            .add(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED)
            .add(Attributes.FOLLOW_RANGE, 16.0)

        @JvmStatic
        fun checkSpawnRules(
            entityType: EntityType<NautilusEntity>,
            level: ServerLevelAccessor,
            spawnType: MobSpawnType,
            pos: BlockPos,
            random: RandomSource,
        ): Boolean = canSpawnInWater(level, pos)

        fun canSpawnInWater(level: ServerLevelAccessor, pos: BlockPos): Boolean =
            level.getFluidState(pos).`is`(FluidTags.WATER) && level.getFluidState(pos.above()).`is`(FluidTags.WATER)
    }
}

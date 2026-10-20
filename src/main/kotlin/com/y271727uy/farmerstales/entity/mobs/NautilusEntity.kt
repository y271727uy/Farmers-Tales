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
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/**
 * A tameable aquatic mount. While a player rides it underwater their air is continually restored,
 * which recreates the useful part of the modern "Breath of the Nautilus" feature on 1.20.1.
 */
open class NautilusEntity(type: EntityType<out NautilusEntity>, level: Level) : TamableAnimal(type, level) {
    init {
        navigation = WaterBoundPathNavigation(this, level)
        moveControl = SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true)
    }

    val isSaddled: Boolean
        get() = entityData.get(SADDLED)

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(SADDLED, false)
    }

    override fun registerGoals() {
        goalSelector.addGoal(0, RandomSwimmingGoal(this, 1.0, 40))
        goalSelector.addGoal(1, LookAtPlayerGoal(this, Player::class.java, 6.0F))
    }

    override fun isFood(stack: ItemStack): Boolean = ModEntities.NAUTILUS.isBreedingFood(stack)

    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? =
        ModEntities.NAUTILUS.type.get().create(level)

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
            } else {
                moveRelative(SWIMMING_ACCELERATION, travelVector)
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
    }

    companion object {
        private const val RIDING_SPEED = 0.18
        private const val SWIMMING_ACCELERATION = 0.03F
        private const val WATER_DRAG = 0.9
        private const val PASSIVE_DESCENT = -0.005
        private val SADDLED: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(NautilusEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder = TamableAnimal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
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

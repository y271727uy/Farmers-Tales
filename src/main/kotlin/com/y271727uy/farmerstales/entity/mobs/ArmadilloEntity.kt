package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.all.ModItems
import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.RandomSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

/** A 1.20.5-style passive armadillo with a defensive roll and periodic scute shedding. */
class ArmadilloEntity(type: EntityType<out ArmadilloEntity>, level: Level) : Animal(type, level) {
    private var rollTicks = 0
    private var nextScuteTick = 12_000 + random.nextInt(12_001)

    val isRolledUp: Boolean
        get() = entityData.get(ROLLED_UP)

    override fun defineSynchedData() {
        super.defineSynchedData()
        entityData.define(ROLLED_UP, false)
    }

    override fun registerGoals() {
        ModEntities.ARMADILLO.registerAnimalGoals(this)
    }

    override fun isFood(stack: ItemStack): Boolean = ModEntities.ARMADILLO.isBreedingFood(stack)

    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? =
        ModEntities.ARMADILLO.type.get().create(level)

    override fun tick() {
        super.tick()
        if (level().isClientSide) return

        if (rollTicks > 0) {
            navigation.stop()
            setDeltaMovement(Vec3.ZERO)
            rollTicks--
            if (rollTicks == 0) entityData.set(ROLLED_UP, false)
        }

        if (!isBaby && tickCount >= nextScuteTick) {
            spawnAtLocation(ModItems.ARMADILLO_SCUTE.get())
            playSound(SoundEvents.TURTLE_EGG_CRACK, 0.45F, 1.3F)
            nextScuteTick = tickCount + 12_000 + random.nextInt(12_001)
        }
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean {
        val damaged = super.hurt(source, amount)
        if (damaged && !isBaby) {
            rollTicks = ROLL_DURATION_TICKS
            entityData.set(ROLLED_UP, true)
            navigation.stop()
            playSound(SoundEvents.TURTLE_HURT, 0.55F, 1.15F)
        }
        return damaged
    }

    override fun travel(travelVector: Vec3) {
        if (!isRolledUp) super.travel(travelVector)
    }

    override fun getAmbientSound(): SoundEvent = SoundEvents.TURTLE_AMBIENT_LAND

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.TURTLE_HURT

    override fun getDeathSound(): SoundEvent = SoundEvents.TURTLE_DEATH

    override fun playStepSound(pos: BlockPos, block: BlockState) {
        playSound(SoundEvents.TURTLE_SHAMBLE, 0.18F, 1.25F)
    }

    override fun isPushable(): Boolean = !isRolledUp

    override fun addAdditionalSaveData(tag: CompoundTag) {
        super.addAdditionalSaveData(tag)
        tag.putInt("RollTicks", rollTicks)
        tag.putInt("NextScuteTick", nextScuteTick)
    }

    override fun readAdditionalSaveData(tag: CompoundTag) {
        super.readAdditionalSaveData(tag)
        rollTicks = tag.getInt("RollTicks")
        nextScuteTick = tag.getInt("NextScuteTick").takeIf { it > tickCount } ?: (tickCount + 12_000 + random.nextInt(12_001))
        entityData.set(ROLLED_UP, rollTicks > 0)
    }

    companion object {
        private const val ROLL_DURATION_TICKS = 160
        private val ROLLED_UP: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(ArmadilloEntity::class.java, EntityDataSerializers.BOOLEAN)

        fun createAttributes(): AttributeSupplier.Builder = Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.19)
            .add(Attributes.FOLLOW_RANGE, 12.0)

        @JvmStatic
        fun checkSpawnRules(
            entityType: EntityType<ArmadilloEntity>,
            level: ServerLevelAccessor,
            spawnType: MobSpawnType,
            pos: BlockPos,
            random: RandomSource,
        ): Boolean = Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random)
    }
}

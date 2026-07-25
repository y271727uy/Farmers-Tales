package com.y271727uy.farmerstales.gameplay.fish_group.entity

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.AABB

open class FishPoolBaseEntity(type: EntityType<out FishPoolBaseEntity>, level: Level) : Entity(type, level) {
    var randomRotation: Float = random.nextFloat() * 360.0F
        private set
    private var interactions = 0
    private var lifeTicks = 0
    private var maxLifeTicks = 9_600

    override fun defineSynchedData() {
        entityData.define(IS_DESTROYING, false)
        entityData.define(DESTRUCTION_PROGRESS, 0.0F)
        entityData.define(HURT_TIME, 0)
        entityData.define(HOOK_INTERACTING_TICKS, 0)
    }

    override fun tick() {
        super.tick()
        lifeTicks++
        if (!level().isClientSide) {
            if (lifeTicks == 1) spawnPlacementParticles()
            if (lifeTicks >= maxLifeTicks) {
                remove(RemovalReason.DISCARDED)
                return
            }
        }
        if (level().isClientSide && tickCount % 120 == 0) spawnPeriodicParticles()

        entityData.get(HURT_TIME).takeIf { it > 0 }?.let { entityData.set(HURT_TIME, it - 1) }
        entityData.get(HOOK_INTERACTING_TICKS).takeIf { it > 0 }?.let {
            entityData.set(HOOK_INTERACTING_TICKS, it - 1)
        }
        if (!level().isClientSide && !isAboveWater()) {
            remove(RemovalReason.DISCARDED)
            return
        }
        if (entityData.get(IS_DESTROYING)) {
            val progress = entityData.get(DESTRUCTION_PROGRESS) + DESTRUCTION_SPEED
            entityData.set(DESTRUCTION_PROGRESS, progress)
            if (progress >= 1.0F) remove(RemovalReason.DISCARDED)
        }
    }

    private fun isAboveWater(): Boolean = level().getFluidState(blockPosition().below()).isSource

    override fun readAdditionalSaveData(compound: CompoundTag) {
        interactions = compound.getInt("Interactions")
        entityData.set(IS_DESTROYING, compound.getBoolean("IsDestroying"))
        entityData.set(DESTRUCTION_PROGRESS, compound.getFloat("DestructionProgress"))
        entityData.set(HURT_TIME, compound.getInt("HurtTime"))
        entityData.set(HOOK_INTERACTING_TICKS, compound.getInt("HookInteractingTicks"))
        lifeTicks = compound.getInt("LifeTicks")
        maxLifeTicks =
            if (compound.contains("MaxLifeTicks", Tag.TAG_INT.toInt())) compound.getInt("MaxLifeTicks") else 9_600
        if (compound.contains("RandomRotation", Tag.TAG_FLOAT.toInt())) {
            randomRotation = compound.getFloat("RandomRotation")
        }
    }

    override fun addAdditionalSaveData(compound: CompoundTag) {
        compound.putInt("Interactions", interactions)
        compound.putBoolean("IsDestroying", entityData.get(IS_DESTROYING))
        compound.putFloat("DestructionProgress", entityData.get(DESTRUCTION_PROGRESS))
        compound.putInt("HurtTime", entityData.get(HURT_TIME))
        compound.putInt("HookInteractingTicks", entityData.get(HOOK_INTERACTING_TICKS))
        compound.putInt("LifeTicks", lifeTicks)
        compound.putInt("MaxLifeTicks", maxLifeTicks)
        compound.putFloat("RandomRotation", randomRotation)
    }

    fun triggerHurt() {
        entityData.set(HURT_TIME, 10)
        if (!level().isClientSide) maxLifeTicks += 2_400
    }

    open fun getLootTable(serverLevel: ServerLevel): LootTable = serverLevel.server.lootData.getLootTable(
        ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "gameplay/fishing_pools/fish_pool_base"),
    )

    open fun getHookInteractionBounds(): AABB = boundingBox.inflate(0.5)

    fun markHookInteracting() {
        entityData.set(
            HOOK_INTERACTING_TICKS,
            maxOf(entityData.get(HOOK_INTERACTING_TICKS), HOOK_INTERACTING_DURATION),
        )
    }

    fun isHookInteracting(): Boolean = entityData.get(HOOK_INTERACTING_TICKS) > 0

    open fun onFishHookInteract(player: Player) {
        val serverLevel = level() as? ServerLevel ?: return
        markHookInteracting()
        val lootParams = LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.THIS_ENTITY, this)
            .withParameter(LootContextParams.ORIGIN, position())
            .withParameter(LootContextParams.KILLER_ENTITY, player)
            .withParameter(LootContextParams.DAMAGE_SOURCE, serverLevel.damageSources().generic())
            .create(LootContextParamSets.ENTITY)
        getLootTable(serverLevel).getRandomItems(lootParams).forEach { stack ->
            if (!player.addItem(stack)) player.drop(stack, false)
        }
        triggerHurt()
        interactions++
        if (interactions >= MAX_INTERACTIONS) removeWithEffects(serverLevel)
    }

    fun removeWithEffects(serverLevel: ServerLevel) {
        serverLevel.playSound(
            null,
            x,
            y,
            z,
            SoundEvents.GOAT_SCREAMING_HORN_BREAK,
            SoundSource.BLOCKS,
            1.0F,
            1.0F,
        )
        repeat(35) {
            val yOffset = serverLevel.random.nextDouble() * 15.0
            val xOffset = 0.25 * (serverLevel.random.nextDouble() - 0.5)
            val zOffset = 0.25 * (serverLevel.random.nextDouble() - 0.5)
            val velocityY = 0.1 + serverLevel.random.nextDouble() * 0.2
            serverLevel.sendParticles(
                ParticleTypes.SPLASH,
                x + xOffset + 0.5,
                y + yOffset,
                z + zOffset + 0.5,
                1,
                0.0,
                velocityY,
                0.0,
                0.0
            )
        }
        repeat(18) {
            val xOffset = serverLevel.random.nextGaussian() * 0.2
            val yOffset = serverLevel.random.nextGaussian() * 0.2
            val zOffset = serverLevel.random.nextGaussian() * 0.2
            serverLevel.sendParticles(ParticleTypes.BUBBLE_POP, x, y, z, 20, xOffset, yOffset, zOffset, 0.1)
            serverLevel.sendParticles(ParticleTypes.POOF, x, y + 1, z, 10, xOffset, yOffset, zOffset, 0.1)
            serverLevel.sendParticles(ParticleTypes.CRIT, x, y, z, 5, xOffset, yOffset, zOffset, 0.1)
        }
        entityData.set(IS_DESTROYING, true)
    }

    val hurtTime: Int
        get() = entityData.get(HURT_TIME)

    protected val isDestroying: Boolean
        get() = entityData.get(IS_DESTROYING)

    fun triggerInteraction() {
        if (isRemoved || isDestroying) return
        interactions++
        triggerHurt()
        if (interactions >= MAX_INTERACTIONS) (level() as? ServerLevel)?.let(::removeWithEffects)
    }

    private fun spawnPlacementParticles() {
        val serverLevel = level() as? ServerLevel ?: return
        repeat(10) {
            val xOffset = random.nextDouble() - 0.5
            val zOffset = random.nextDouble() - 0.5
            serverLevel.sendParticles(ParticleTypes.BUBBLE, x + xOffset, y + 1.9, z + zOffset, 1, 0.0, 0.1, 0.0, 0.0)
        }
    }

    private fun spawnPeriodicParticles() {
        val particleX = x + (random.nextDouble() - 0.5) * 2.0
        val particleZ = z + (random.nextDouble() - 0.5) * 2.0
        level().addParticle(ParticleTypes.SPLASH, particleX, y + 1.8, particleZ, 0.0, 0.05, 0.0)
    }

    override fun getBoundingBoxForCulling(): AABB = AABB(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1)

    companion object {
        private const val MAX_INTERACTIONS = 3
        private const val HOOK_INTERACTING_DURATION = 10
        private const val DESTRUCTION_SPEED = 0.05F
        private val IS_DESTROYING: EntityDataAccessor<Boolean> =
            SynchedEntityData.defineId(FishPoolBaseEntity::class.java, EntityDataSerializers.BOOLEAN)
        private val DESTRUCTION_PROGRESS: EntityDataAccessor<Float> =
            SynchedEntityData.defineId(FishPoolBaseEntity::class.java, EntityDataSerializers.FLOAT)
        private val HURT_TIME: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FishPoolBaseEntity::class.java, EntityDataSerializers.INT)
        private val HOOK_INTERACTING_TICKS: EntityDataAccessor<Int> =
            SynchedEntityData.defineId(FishPoolBaseEntity::class.java, EntityDataSerializers.INT)
    }
}

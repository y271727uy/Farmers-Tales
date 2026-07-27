package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.entity.ModEntities
import net.minecraft.core.BlockPos
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

class BambooRatEntity(type: EntityType<out BambooRatEntity>, level: Level) : Animal(type, level) {
    override fun registerGoals() {
        ModEntities.BAMBOO_RAT.registerAnimalGoals(this)
    }

    override fun isFood(stack: ItemStack): Boolean = ModEntities.BAMBOO_RAT.isBreedingFood(stack)

    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? =
        ModEntities.BAMBOO_RAT.type.get().create(level)

    override fun getAmbientSound(): SoundEvent = SoundEvents.RABBIT_AMBIENT

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.RABBIT_HURT

    override fun getDeathSound(): SoundEvent = SoundEvents.RABBIT_DEATH

    override fun playStepSound(pos: BlockPos, block: BlockState) {
        playSound(SoundEvents.RABBIT_JUMP, 0.15F, 1.0F)
    }

    companion object {
        fun createAttributes(): AttributeSupplier.Builder = Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 10.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.FOLLOW_RANGE, 10.0)

        @JvmStatic
        fun checkSpawnRules(
            entityType: EntityType<BambooRatEntity>,
            level: ServerLevelAccessor,
            spawnType: MobSpawnType,
            pos: BlockPos,
            random: RandomSource,
        ): Boolean = Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random)
    }
}

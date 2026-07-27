package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.entity.ModEntities
import com.y271727uy.farmerstales.entity.ai.RatCatInteractions
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundEvents
import net.minecraft.util.RandomSource
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.TamableAnimal
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.state.BlockState

class RatEntity(type: EntityType<out RatEntity>, level: Level) : TamableAnimal(type, level) {
    override fun registerGoals() {
        ModEntities.RAT.registerAnimalGoals(this)
        goalSelector.addGoal(1, SitWhenOrderedToGoal(this))
        goalSelector.addGoal(2, MeleeAttackGoal(this, 1.25, true))
        goalSelector.addGoal(3, FollowOwnerGoal(this, 1.2, 8.0F, 3.0F, false))
        targetSelector.addGoal(1, OwnerHurtByTargetGoal(this))
    }

    override fun isFood(stack: ItemStack): Boolean = ModEntities.RAT.isBreedingFood(stack)

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (!isTame && isFood(stack)) {
            if (!level().isClientSide) {
                usePlayerItem(player, hand, stack)
                if (random.nextInt(3) == 0) {
                    tame(player)
                    setOrderedToSit(true)
                    setInSittingPose(true)
                    navigation.stop()
                    RatCatInteractions.clearCatTargets(this)
                    level().broadcastEntityEvent(this, 7.toByte())
                } else {
                    level().broadcastEntityEvent(this, 6.toByte())
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide)
        }

        if (isTame && isOwnedBy(player) && isFood(stack)) {
            return ModEntities.RAT.healTamedAnimal(this, player, hand) ?: super.mobInteract(player, hand)
        }

        val result = super.mobInteract(player, hand)
        if (isTame && isOwnedBy(player) && !result.consumesAction()) {
            val shouldSit = !isOrderedToSit
            setOrderedToSit(shouldSit)
            setInSittingPose(shouldSit)
            navigation.stop()
            player.displayClientMessage(
                Component.translatable("message.farmerstales.rat.command.${if (shouldSit) "stay" else "follow"}"),
                true,
            )
            return InteractionResult.SUCCESS
        }
        return result
    }

    override fun hurt(source: DamageSource, amount: Float): Boolean =
        super.hurt(source, if (source.entity is Cat) maxOf(amount, health) else amount)

    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? =
        ModEntities.RAT.type.get().create(level)

    override fun getAmbientSound(): SoundEvent = SoundEvents.RABBIT_AMBIENT

    override fun getHurtSound(damageSource: DamageSource): SoundEvent = SoundEvents.RABBIT_HURT

    override fun getDeathSound(): SoundEvent = SoundEvents.RABBIT_DEATH

    override fun playStepSound(pos: BlockPos, block: BlockState) {
        playSound(SoundEvents.RABBIT_JUMP, 0.1F, 1.2F)
    }

    companion object {
        private const val DEFENSE_DAMAGE = 1.0

        fun createAttributes(): AttributeSupplier.Builder = Animal.createLivingAttributes()
            .add(Attributes.MAX_HEALTH, 6.0)
            .add(Attributes.MOVEMENT_SPEED, 0.32)
            .add(Attributes.ATTACK_DAMAGE, DEFENSE_DAMAGE)
            .add(Attributes.ATTACK_KNOCKBACK, 0.0)
            .add(Attributes.FOLLOW_RANGE, 12.0)

        @JvmStatic
        fun checkSpawnRules(
            entityType: EntityType<RatEntity>,
            level: ServerLevelAccessor,
            spawnType: MobSpawnType,
            pos: BlockPos,
            random: RandomSource,
        ): Boolean = RatSpawnRules.canSpawn(level, spawnType, pos)
    }
}

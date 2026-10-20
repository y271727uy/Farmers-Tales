package com.y271727uy.farmerstales.entity.ai

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.animal.Ocelot
import net.minecraft.world.entity.animal.Wolf
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Collections
import java.util.WeakHashMap
import java.util.function.Predicate

/** Adds wild rats as prey without changing the predators' existing attack goals. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object RatPredatorInteractions {
    private const val WILD_PREDATOR_ATTACK_CHANCE = 0.25F
    private val configuredPredators = Collections.newSetFromMap(WeakHashMap<Mob, Boolean>())

    @JvmStatic
    @SubscribeEvent
    fun onPredatorJoinLevel(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide) return
        val predator = event.entity as? Mob ?: return
        if (predator !is IronGolem && predator !is Wolf && predator !is Ocelot) return
        if (!configuredPredators.add(predator)) return

        when (predator) {
            is IronGolem -> predator.targetSelector.addGoal(3, RatTargetGoal(predator, 10, 1.0F))
            is Wolf -> predator.targetSelector.addGoal(6, RatTargetGoal(predator, 20, WILD_PREDATOR_ATTACK_CHANCE))
            is Ocelot -> predator.targetSelector.addGoal(2, RatTargetGoal(predator, 20, WILD_PREDATOR_ATTACK_CHANCE))
        }
    }

    private class RatTargetGoal<T : Mob>(
        private val predator: T,
        interval: Int,
        private val attackChance: Float,
    ) : NearestAttackableTargetGoal<RatEntity>(
        predator,
        RatEntity::class.java,
        interval,
        true,
        false,
        Predicate<LivingEntity> { target -> target is RatEntity && !target.isTame },
    ) {
        override fun canUse(): Boolean = predator.random.nextFloat() < attackChance && super.canUse()

        override fun canContinueToUse(): Boolean =
            (predator.target as? RatEntity)?.isTame == false && super.canContinueToUse()
    }
}

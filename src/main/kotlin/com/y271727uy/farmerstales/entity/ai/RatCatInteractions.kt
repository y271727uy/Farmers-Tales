package com.y271727uy.farmerstales.entity.ai

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.RatEntity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.Cat
import net.minecraft.world.phys.AABB
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Collections
import java.util.WeakHashMap
import java.util.function.Predicate

/** Makes every cat recognize rats as prey while retaining Minecraft's built-in leap and ocelot attack goals. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object RatCatInteractions {
    private val configuredCats = Collections.newSetFromMap(WeakHashMap<Cat, Boolean>())

    @JvmStatic
    @SubscribeEvent
    fun onCatJoinLevel(event: EntityJoinLevelEvent) {
        if (event.level.isClientSide) return
        val cat = event.entity as? Cat ?: return
        if (!configuredCats.add(cat)) return

        cat.targetSelector.addGoal(
            2,
            NearestAttackableTargetGoal(
                cat,
                RatEntity::class.java,
                10,
                true,
                false,
                Predicate<LivingEntity> { target -> target is RatEntity && !target.isTame },
            ),
        )
        cat.goalSelector.addGoal(4, RatPounceGoal(cat))
        cat.goalSelector.addGoal(5, RatPursuitGoal(cat))
    }

    fun clearCatTargets(rat: RatEntity) {
        val level = rat.level()
        if (level.isClientSide) return
        level.getEntitiesOfClass(Cat::class.java, rat.boundingBox.inflate(32.0)).forEach { cat ->
            if (cat.target === rat) cat.target = null
        }
    }

    private class RatPounceGoal(private val cat: Cat) : LeapAtTargetGoal(cat, 0.4F) {
        override fun canUse(): Boolean = cat.canHuntRat() && super.canUse()

        override fun canContinueToUse(): Boolean = cat.canHuntRat() && super.canContinueToUse()
    }

    private class RatPursuitGoal(private val cat: Cat) : MeleeAttackGoal(cat, 1.35, true) {
        override fun canUse(): Boolean = cat.canHuntRat() && super.canUse()

        override fun canContinueToUse(): Boolean = cat.canHuntRat() && super.canContinueToUse()
    }

    private fun Cat.canHuntRat(): Boolean = (target as? RatEntity)?.isTame == false
}

package com.y271727uy.farmerstales.gameplay.fish_group.entity

import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolFactory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.AnimationState
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class RiverFishPoolEntity(type: EntityType<out RiverFishPoolEntity>, level: Level) :
    AbstractFishPoolEntity(type, level) {
    val idleAnimationState = AnimationState()
    private var idleAnimationTimeout = 0

    init {
        setFishPoolDefinition(resolveDefinitionIdFromType(type))
    }

    override val environment: FishPoolDefinition.Environment
        get() = FishPoolDefinition.Environment.RIVER
    override val defaultFishPoolId: ResourceLocation
        get() = FishPoolFactory.RIVER_FISH_POOL.id

    override fun tick() {
        super.tick()
        if (level().isClientSide) updateAnimations()
    }

    private fun updateAnimations() {
        if (idleAnimationTimeout <= 0) {
            idleAnimationTimeout = 200
            idleAnimationState.start(tickCount)
        } else {
            idleAnimationTimeout--
        }
    }
}

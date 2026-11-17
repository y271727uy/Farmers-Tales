package com.y271727uy.farmerstales.gameplay.fish_group.entity

import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolFactory
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class RiverFishPoolEntity(type: EntityType<out RiverFishPoolEntity>, level: Level) :
    AbstractFishPoolEntity(type, level) {
    init {
        setFishPoolDefinition(resolveDefinitionIdFromType(type))
    }

    override val environment: FishPoolDefinition.Environment
        get() = FishPoolDefinition.Environment.RIVER
    override val defaultFishPoolId: ResourceLocation
        get() = FishPoolFactory.RIVER_FISH_POOL.id
}

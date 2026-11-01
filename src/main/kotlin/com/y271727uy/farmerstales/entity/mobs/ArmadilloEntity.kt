package com.y271727uy.farmerstales.entity.mobs

import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor

/** Keeps the original entity registration and API while using the completed backport behavior. */
class ArmadilloEntity(type: EntityType<out ArmadilloEntity>, level: Level) :
    com.y271727uy.farmerstales.gameplay.fauna.ArmadilloEntity(type, level) {
    val isRolledUp: Boolean get() = rolled()
    companion object {
        fun createAttributes(): AttributeSupplier.Builder = com.y271727uy.farmerstales.gameplay.fauna.ArmadilloEntity.attributes()
        @JvmStatic fun checkSpawnRules(type: EntityType<ArmadilloEntity>, level: ServerLevelAccessor, reason: MobSpawnType, pos: BlockPos, random: RandomSource): Boolean =
            com.y271727uy.farmerstales.gameplay.fauna.ArmadilloEntity.spawnRules(type, level, reason, pos, random)
    }
}

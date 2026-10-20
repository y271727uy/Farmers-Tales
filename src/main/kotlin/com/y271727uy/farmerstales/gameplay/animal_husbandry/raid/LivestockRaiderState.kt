package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.animal.Wolf

/** Marks event-spawned livestock raiders in Forge persistent entity data. */
object LivestockRaiderState {
    private const val DATA_KEY = "farmerstales.livestock_raider"

    @JvmStatic
    fun isRaider(entity: LivingEntity): Boolean = entity.persistentData.getBoolean(DATA_KEY)

    fun mark(entity: LivingEntity) {
        entity.persistentData.putBoolean(DATA_KEY, true)
    }

    fun canDespawn(entity: LivingEntity): Boolean {
        if (!isRaider(entity) || !entity.isAlive || entity.hasCustomName()) return false
        if (entity is Mob && entity.isLeashed) return false
        if (entity is Wolf && entity.isTame) return false
        return true
    }
}

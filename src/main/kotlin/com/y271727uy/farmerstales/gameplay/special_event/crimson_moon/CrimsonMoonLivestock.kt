package com.y271727uy.farmerstales.gameplay.special_event.crimson_moon

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.animal.Chicken

/** Java mixin bridge. CrimsonMoonEvents is a Kotlin object and is not a stable Java call target. */
object CrimsonMoonLivestock {
    @JvmStatic
    fun shouldHoldEgg(chicken: Chicken, eggTime: Int): Boolean {
        if (chicken.level().isClientSide || chicken.isBaby || eggTime > 1) return false
        val level = chicken.level() as? ServerLevel ?: return false
        return CrimsonMoonEvents.isActive(level)
    }
}

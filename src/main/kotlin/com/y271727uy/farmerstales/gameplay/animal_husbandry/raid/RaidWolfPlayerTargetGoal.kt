package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.entity.player.Player

/** Raid wolves aggro nearby players instead of waiting to be hit first. */
class RaidWolfPlayerTargetGoal(wolf: Wolf) : NearestAttackableTargetGoal<Player>(
    wolf,
    Player::class.java,
    10,
    true,
    false,
    null,
) {
    override fun canUse(): Boolean = isRaidHunter() && super.canUse()

    override fun canContinueToUse(): Boolean = isRaidHunter() && super.canContinueToUse()

    private fun isRaidHunter(): Boolean {
        val wolf = mob as Wolf
        if (wolf.isTame || !LivestockRaiderState.isRaider(wolf)) return false
        if (RaidWolfDeterrence.shouldFlee(wolf)) return false
        val level = wolf.level() as? ServerLevel ?: return false
        return !PredatorRaidEvents.areWolfRaidersLeaving(level)
    }
}

package com.y271727uy.farmerstales.entity.mobs

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.AgeableMob
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

/** Undead nautical mount used by naturally spawned drowned; it cannot be tamed or bred. */
class ZombieNautilusEntity(type: EntityType<out ZombieNautilusEntity>, level: Level) : NautilusEntity(type, level) {
    override fun isFood(stack: ItemStack): Boolean = false

    override fun mobInteract(player: Player, hand: InteractionHand): InteractionResult = InteractionResult.PASS

    override fun getBreedOffspring(level: ServerLevel, otherParent: AgeableMob): AgeableMob? = null
}

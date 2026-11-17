package com.y271727uy.farmerstales.integration.curios

import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import top.theillusivec4.curios.api.CuriosApi

/** Loaded only after checking that Curios is installed. Cosmetic slots do not count as equipped. */
object CuriosItemIntegration {
    fun isEquipped(player: Player, item: Item): Boolean = CuriosApi.getCuriosInventory(player)
        .map { it.isEquipped(item) }.orElse(false)
}

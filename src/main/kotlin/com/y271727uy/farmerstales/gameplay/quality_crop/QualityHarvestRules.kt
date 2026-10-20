package com.y271727uy.farmerstales.gameplay.quality_crop

import com.y271727uy.farmerstales.all.items.ScytheItem
import com.y271727uy.farmerstales.config.Config
import net.minecraft.world.entity.player.Player

/** Determines whether the harvesting player is eligible for quality drops. */
object QualityHarvestRules {
    fun canReceiveQuality(player: Player?): Boolean =
        !Config.requireScytheForCropQuality || player?.mainHandItem?.item is ScytheItem
}

package com.y271727uy.farmerstales.gameplay.advancements

import com.y271727uy.farmerstales.FTMod
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player

/** Centralises event-driven advancement awards for the mod's gameplay systems. */
object FarmersTalesAdvancements {
    fun award(player: ServerPlayer, id: String, criterion: String = CRITERION) {
        val advancement = player.server.advancements.getAdvancement(
            ResourceLocation.fromNamespaceAndPath(FTMod.MODID, id),
        ) ?: return
        val progress = player.advancements.getOrStartProgress(advancement)
        if (progress.isDone) return
        player.advancements.award(advancement, criterion)
    }

    fun awardIfRaining(player: Player?, level: ServerLevel) {
        if (level.isRaining) (player as? ServerPlayer)?.let { award(it, "quality_crop/rain_nourishes") }
    }

    private const val CRITERION = "unlocked"
}

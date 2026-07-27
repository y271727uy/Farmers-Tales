package com.y271727uy.farmerstales.gameplay.season_breeding

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.animal.Animal
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object SeasonBreedingEvents {
    @JvmStatic
    @SubscribeEvent
    fun onBabyEntitySpawn(event: BabyEntitySpawnEvent) {
        val animal = event.parentA as? Animal ?: return
        if (!isLoveBlocked(animal)) {
            return
        }

        event.isCanceled = true
        event.causedByPlayer?.let { SeasonSupport.sendPlayerFeedback(it, blockedMessage(animal)) }
    }

    fun allowedSeasons(animal: Animal) = BreedingSeasonRules.allowedSeasons(animal.type)

    /**
     * Whether this animal is currently forbidden from entering love mode.
     *
     * Feeding must remain available outside the breeding season, so this is
     * called from the actual vanilla love-mode entry point rather than from
     * a player interaction event.
     */
    @JvmStatic
    fun isLoveBlocked(animal: Animal): Boolean {
        if (!Config.restrictAnimalBreeding || !IntegrationManager.isSereneSeasonsLoaded()) {
            return false
        }
        if (!BreedingSeasonRules.hasRule(animal.type)) {
            return false
        }

        val currentSeason = SeasonSupport.currentSeasonWindow(animal.level())
        return currentSeason != null && currentSeason !in allowedSeasons(animal)
    }

    @JvmStatic
    fun sendLoveBlockedFeedback(animal: Animal, player: net.minecraft.world.entity.player.Player) {
        SeasonSupport.sendPlayerFeedback(player, blockedMessage(animal))
    }

    private fun blockedMessage(animal: Animal): Component = Component.translatable(
        "message.${FTMod.MODID}.breeding_blocked",
        SeasonSupport.formatSeasonsInlineOrYearRound(allowedSeasons(animal))
    )
}

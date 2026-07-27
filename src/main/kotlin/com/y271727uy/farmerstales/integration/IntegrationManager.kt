package com.y271727uy.farmerstales.integration

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.farmersdelight.FarmersDelightIntegration
import com.y271727uy.farmerstales.integration.crafttweaker.CraftTweakerIntegration
import com.y271727uy.farmerstales.integration.jade.JadeIntegration
import com.y271727uy.farmerstales.integration.jei.JeiIntegration
import com.y271727uy.farmerstales.integration.kubejs.KubeJsIntegration
import com.y271727uy.farmerstales.integration.list.ListIntegration
import com.y271727uy.farmerstales.integration.sereneseasons.SereneSeasonsIntegration
import net.minecraft.world.level.Level
import net.minecraftforge.fml.ModList
import net.minecraftforge.eventbus.api.IEventBus

object IntegrationManager {
    const val JADE = "jade"
    const val JEI = "jei"
    const val LIST = "list"
    const val FARMERS_DELIGHT = "farmersdelight"
    const val SERENE_SEASONS = "sereneseasons"
    const val KUBEJS = "kubejs"
    const val CRAFTTWEAKER = "crafttweaker"

    fun initCreativeTabs(modEventBus: IEventBus) {
        if (isLoaded(LIST)) {
            ListIntegration.init(modEventBus)
        } else if (isLoaded(FARMERS_DELIGHT)) {
            FarmersDelightIntegration.init(modEventBus)
        }
    }

    fun init() {
        if (isLoaded(JADE)) {
            JadeIntegration.init()
        }
        if (isLoaded(JEI)) {
            JeiIntegration.init()
        }
        if (isLoaded(SERENE_SEASONS)) {
            SereneSeasonsIntegration.init()
        }
        if (isLoaded(KUBEJS)) {
            KubeJsIntegration.init()
        }
        if (isLoaded(CRAFTTWEAKER)) {
            CraftTweakerIntegration.init()
        }
    }

    fun currentSeasonName(level: Level): String? {
        if (!isLoaded(SERENE_SEASONS)) {
            return null
        }
        return SereneSeasonsIntegration.currentSeasonName(level)
    }

    fun currentSeasonId(level: Level?, @Suppress("UNUSED_PARAMETER") pos: net.minecraft.core.BlockPos?): String? {
        return level?.let(::currentSeasonName)
    }

    fun isSereneSeasonsLoaded(): Boolean {
        return ModList.get().isLoaded(SERENE_SEASONS)
    }

    private fun isLoaded(modId: String): Boolean {
        val loaded = ModList.get().isLoaded(modId)
        if (loaded) {
            FTMod.LOGGER.debug("Integration mod loaded: {}", modId)
        }
        return loaded
    }
}

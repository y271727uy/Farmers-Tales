package com.y271727uy.farmerstales.integration.kubejs

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.special_event.seasonal.SeasonalEvents
import dev.latvian.mods.kubejs.KubeJSPlugin
import dev.latvian.mods.kubejs.script.BindingsEvent

/**
 * Loaded only by KubeJS through kubejs.plugins.txt. Keep every direct KubeJS
 * API call in this class or other classes reachable exclusively from it.
 */
class FarmersTalesKubeJsPlugin : KubeJSPlugin() {
    override fun init() {
        FTMod.LOGGER.debug("Registered Farmer's Tales KubeJS plugin")
    }

    override fun registerBindings(event: BindingsEvent) {
        event.add("FarmersTalesSeasonalEvents", SeasonalEvents)
    }
}

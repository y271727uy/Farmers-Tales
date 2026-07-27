package com.y271727uy.farmerstales.integration.crafttweaker

import com.y271727uy.farmerstales.FTMod

/**
 * Kept free of CraftTweaker API types so normal mod initialization remains
 * safe when CraftTweaker is absent. Script-facing types are discovered by
 * CraftTweaker through their annotations.
 */
object CraftTweakerIntegration {
    fun init() {
        FTMod.LOGGER.debug("CraftTweaker integration enabled")
    }
}

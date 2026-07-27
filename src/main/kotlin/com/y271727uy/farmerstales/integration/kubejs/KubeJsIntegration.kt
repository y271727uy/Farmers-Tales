package com.y271727uy.farmerstales.integration.kubejs

import com.y271727uy.farmerstales.FTMod

/**
 * The common entry point deliberately contains no KubeJS API types, so it can
 * be loaded safely when KubeJS is absent. API-facing code belongs in the
 * KubeJS plugin registered through kubejs.plugins.txt.
 */
object KubeJsIntegration {
    fun init() {
        FTMod.LOGGER.debug("KubeJS integration enabled")
    }
}

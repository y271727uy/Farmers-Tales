package com.y271727uy.farmerstales.gameplay.gene_crop

/** Shared by server growth checks and the server-supplied Jade status. */
enum class CropTemperatureStatus(val growthMultiplier: Float, val translationSuffix: String) {
    SUITABLE(1.0F, "suitable"),
    CHILLY(0.5F, "chilly"),
    WARM(0.5F, "warm"),
    TOO_COLD(0.0F, "too_cold"),
    TOO_HOT(0.0F, "too_hot"),
}

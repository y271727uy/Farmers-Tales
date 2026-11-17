package com.y271727uy.farmerstales.gameplay.common

/** Channel access for packed `0xRRGGBB` colours, as `0..1` floats. */
object Rgb {
    @JvmStatic
    fun red(rgb: Int): Float = ((rgb shr 16) and 0xFF) / 255f

    @JvmStatic
    fun green(rgb: Int): Float = ((rgb shr 8) and 0xFF) / 255f

    @JvmStatic
    fun blue(rgb: Int): Float = (rgb and 0xFF) / 255f
}

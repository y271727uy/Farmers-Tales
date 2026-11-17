package com.y271727uy.farmerstales.gameplay.common.client

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.client.event.ViewportEvent

/** A weather colour that washes over sky, clouds and fog by a per-surface amount. */
class SkyTint(private val red: Float, private val green: Float, private val blue: Float) {
    private val color = Vec3(red.toDouble(), green.toDouble(), blue.toDouble())

    fun mix(original: Vec3, amount: Double): Vec3 = original.lerp(color, amount)

    fun mixFog(event: ViewportEvent.ComputeFogColor, amount: Float) {
        event.red = Mth.lerp(amount, event.red, red)
        event.green = Mth.lerp(amount, event.green, green)
        event.blue = Mth.lerp(amount, event.blue, blue)
    }
}

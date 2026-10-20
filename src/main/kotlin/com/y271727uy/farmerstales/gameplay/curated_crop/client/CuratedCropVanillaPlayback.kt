package com.y271727uy.farmerstales.gameplay.curated_crop.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.curated_crop.CuratedCropParticles
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.network.chat.Component

/** Used only when Photon is not installed. */
object CuratedCropVanillaPlayback {
    fun play(pos: BlockPos, kind: String, reason: String? = null) {
        if (reason != null) {
            FTMod.LOGGER.warn("Curated crop fell back to vanilla particles: {}", reason)
            Minecraft.getInstance().player?.sendSystemMessage(
                Component.literal("Photon 没生效，已改用原版粒子：$reason"),
            )
        }
        val level = Minecraft.getInstance().level ?: return
        val types = types(kind)
        if (types.isEmpty()) return
        val x = pos.x + 0.5
        val y = pos.y.toDouble()
        val z = pos.z + 0.5
        types.forEach { type ->
            repeat(if (kind == "all") 6 else 12) {
                level.addParticle(type, x, y, z, 0.0, 0.0, 0.0)
            }
        }
    }

    private fun types(kind: String): List<SimpleParticleType> = when (kind) {
        "moon" -> listOf(CuratedCropParticles.MOON.get())
        "rainbow" -> listOf(CuratedCropParticles.RAINBOW.get())
        "popular" -> listOf(CuratedCropParticles.POPULAR.get())
        "stars" -> listOf(CuratedCropParticles.STARS.get())
        "all" -> listOf(
            CuratedCropParticles.MOON.get(),
            CuratedCropParticles.RAINBOW.get(),
            CuratedCropParticles.POPULAR.get(),
            CuratedCropParticles.STARS.get(),
        )
        else -> emptyList()
    }
}

package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import com.y271727uy.farmerstales.network.GeneCropQueryPacket
import com.y271727uy.farmerstales.gameplay.gene_crop.CropTemperatureStatus
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level

/** Remembers the last answer for each potato so Jade does not ask every frame. */
object GeneCropJadeCache {
    private const val REFRESH_MS = 1000L

    private val answers = HashMap<Long, Boolean>()
    private val temperatures = HashMap<Long, CropTemperatureStatus>()
    private val askedAt = HashMap<Long, Long>()
    private var dimension: String? = null

    fun prepare(level: Level) {
        val id = level.dimension().location().toString()
        if (dimension == id) {
            return
        }
        dimension = id
        answers.clear()
        temperatures.clear()
        askedAt.clear()
    }

    fun identified(pos: BlockPos): Boolean? = answers[pos.asLong()]
    fun temperature(pos: BlockPos): CropTemperatureStatus? = temperatures[pos.asLong()]

    fun request(pos: BlockPos) {
        val now = System.currentTimeMillis()
        val key = pos.asLong()
        val last = askedAt[key]
        if (last != null && now - last < REFRESH_MS) {
            return
        }
        askedAt[key] = now
        FarmerstalesNetwork.CHANNEL.sendToServer(GeneCropQueryPacket(pos))
    }

    fun accept(pos: BlockPos, identified: Boolean, temperature: CropTemperatureStatus) {
        val key = pos.asLong()
        if (!askedAt.containsKey(key)) {
            return
        }
        answers[key] = identified
        temperatures[key] = temperature
    }
}

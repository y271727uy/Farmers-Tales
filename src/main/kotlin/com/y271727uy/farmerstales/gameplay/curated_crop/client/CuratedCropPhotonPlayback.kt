package com.y271727uy.farmerstales.gameplay.curated_crop.client

import net.minecraft.core.BlockPos

// TODO: 恢复 Photon 支持后取消注释
object CuratedCropPhotonPlayback {
    fun play(pos: BlockPos, kind: String) {
        // Photon 暂时禁用
        CuratedCropVanillaPlayback.play(pos, kind, "Photon 暂时禁用")
    }
}

/*
原始 Photon 实现已注释 - 等待 LDLib 重映射问题解决后恢复

import com.lowdragmc.photon.client.fx.BlockEffect
import com.lowdragmc.photon.client.fx.FX
...
*/

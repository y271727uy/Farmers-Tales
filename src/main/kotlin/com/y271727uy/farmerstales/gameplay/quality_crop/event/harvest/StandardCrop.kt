package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block

/** Shared mature-crop loot helper used by the scythe path. */
object StandardCrop {
    /** Returns mature-crop loot after applying this mod's crop quality rules. */
    @JvmStatic
    fun getHarvestDrops(
        level: ServerLevel,
        pos: net.minecraft.core.BlockPos,
        state: net.minecraft.world.level.block.state.BlockState,
        shouldApplyQuality: Boolean,
        harvester: net.minecraft.world.entity.player.Player? = null,
    ) =
        Block.getDrops(state, level, pos, level.getBlockEntity(pos)).onEach { stack ->
            FarmersTalesAdvancements.awardIfRaining(harvester, level)
            if (shouldApplyQuality) {
                QualityDropProcessor.applyStandard(stack, harvester, level.random)
            }
        }
}

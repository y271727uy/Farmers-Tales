package com.y271727uy.farmerstales.gameplay.special_event.seasonal

import com.y271727uy.farmerstales.integration.IntegrationManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.state.BlockState

object WarmCurrent {
    /** For an already ripe fruit, retain the usual season and greenhouse rules outside warm currents. */
    fun canPick(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean {
        if (level.dimension() != Level.OVERWORLD) return true
        if (state.block !is LeavesBlock && !state.`is`(BlockTags.LEAVES)) return true
        return SeasonalEvents.isActive(level, SeasonalEvent.WARM_CURRENT) ||
            IntegrationManager.isFruitInSeason(level, pos, state)
    }
}

package com.y271727uy.farmerstales.gameplay.common

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel

/**
 * Grants a block one extra random tick from inside a crop-growth handler.
 *
 * The extra tick fires the growth event again, so each bonus source keeps its
 * own guard: it never re-enters itself, while separate sources still stack.
 */
class BonusGrowthTick {
    private val applying = ThreadLocal.withInitial { false }

    /** True while this source's own extra tick is running. */
    val isApplying: Boolean get() = applying.get()

    /** Returns false when the block does not random-tick or this source is already applying. */
    fun apply(level: ServerLevel, pos: BlockPos): Boolean {
        if (applying.get()) return false
        val state = level.getBlockState(pos)
        if (!state.isRandomlyTicking) return false

        applying.set(true)
        try {
            state.randomTick(level, pos, level.random)
        } finally {
            applying.set(false)
        }
        return true
    }
}

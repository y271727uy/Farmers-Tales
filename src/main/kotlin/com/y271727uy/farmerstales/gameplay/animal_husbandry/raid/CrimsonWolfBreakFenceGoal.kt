package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdingRules
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.BlockTags
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.ai.goal.Goal
import net.minecraft.world.entity.animal.Sheep
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.level.block.FenceBlock
import net.minecraft.world.level.block.FenceGateBlock
import java.util.EnumSet
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.min

/** Crimson-moon raid wolves may smash one fence or gate between themselves and a sheep. */
class CrimsonWolfBreakFenceGoal(private val wolf: Wolf) : Goal() {
    private var fencePos: BlockPos? = null
    private var breakTicks = 0
    private var searchCooldown = 0

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (!canAttempt()) return false
        if (searchCooldown > 0) {
            searchCooldown--
            return false
        }
        val pos = findBlockingFence() ?: run {
            searchCooldown = SEARCH_INTERVAL_TICKS
            return false
        }
        fencePos = pos
        return true
    }

    override fun canContinueToUse(): Boolean {
        val pos = fencePos ?: return false
        return canAttempt() && isBreakableFence(pos)
    }

    override fun start() {
        breakTicks = 0
    }

    override fun stop() {
        fencePos?.let { pos ->
            wolf.level().destroyBlockProgress(wolf.id, pos, -1)
        }
        fencePos = null
        breakTicks = 0
        searchCooldown = SEARCH_INTERVAL_TICKS
        wolf.navigation.stop()
    }

    override fun tick() {
        val pos = fencePos ?: return
        val level = wolf.level() as? ServerLevel ?: return
        val x = pos.x + 0.5
        val y = pos.y + 0.5
        val z = pos.z + 0.5
        wolf.lookControl.setLookAt(x, y, z)
        if (wolf.distanceToSqr(x, y, z) > REACH_DISTANCE_SQR) {
            wolf.navigation.moveTo(x, pos.y.toDouble(), z, APPROACH_SPEED)
            return
        }

        wolf.navigation.stop()
        breakTicks++
        if (breakTicks % HIT_INTERVAL_TICKS == 0) {
            wolf.swing(InteractionHand.MAIN_HAND)
            val state = level.getBlockState(pos)
            level.playSound(
                null,
                pos,
                state.soundType.hitSound,
                SoundSource.HOSTILE,
                0.8F,
                0.8F + level.random.nextFloat() * 0.4F,
            )
            level.sendParticles(
                BlockParticleOption(ParticleTypes.BLOCK, state),
                x,
                y,
                z,
                8,
                0.25,
                0.25,
                0.25,
                0.05,
            )
            level.destroyBlockProgress(wolf.id, pos, (breakTicks * 10 / BREAK_TICKS).coerceAtMost(9))
        }
        if (breakTicks < BREAK_TICKS) return
        if (!PredatorRaidEvents.hasFenceBreakQuota(level)) return
        PredatorRaidEvents.consumeFenceBreakQuota(level)
        level.destroyBlock(pos, true, wolf)
        level.destroyBlockProgress(wolf.id, pos, -1)
        fencePos = null
    }

    override fun requiresUpdateEveryTick(): Boolean = true

    private fun canAttempt(): Boolean {
        if (wolf.level().isClientSide || wolf.isTame || !LivestockRaiderState.isRaider(wolf)) return false
        if (RaidWolfDeterrence.shouldFlee(wolf)) return false
        val level = wolf.level() as? ServerLevel ?: return false
        if (!PredatorRaidEvents.hasFenceBreakQuota(level)) return false
        val sheep = wolf.target as? Sheep ?: return false
        return sheep.isAlive && ShepherdingRules.isEligible(sheep)
    }

    private fun findBlockingFence(): BlockPos? {
        val sheep = wolf.target as? Sheep ?: return null
        val dx = sheep.x - wolf.x
        val dy = sheep.y - wolf.y
        val dz = sheep.z - wolf.z
        val dist = hypot(dx, dz)
        if (dist < 1.0) return null

        val steps = min(SCAN_STEPS, ceil(dist).toInt()).coerceAtLeast(1)
        val stepX = dx / steps
        val stepY = dy / steps
        val stepZ = dz / steps
        var x = wolf.x
        var y = wolf.y
        var z = wolf.z
        repeat(steps) {
            x += stepX
            y += stepY
            z += stepZ
            val base = BlockPos.containing(x, y, z)
            for (oy in -1..1) {
                val pos = base.offset(0, oy, 0)
                if (isBreakableFence(pos)) return pos.immutable()
            }
        }
        return null
    }

    private fun isBreakableFence(pos: BlockPos): Boolean {
        val state = wolf.level().getBlockState(pos)
        val block = state.block
        return block is FenceBlock ||
            block is FenceGateBlock ||
            state.`is`(BlockTags.FENCES) ||
            state.`is`(BlockTags.FENCE_GATES)
    }

    companion object {
        private const val SEARCH_INTERVAL_TICKS = 5
        private const val BREAK_TICKS = 40
        private const val HIT_INTERVAL_TICKS = 5
        private const val REACH_DISTANCE_SQR = 2.25 * 2.25
        private const val APPROACH_SPEED = 1.2
        private const val SCAN_STEPS = 12
    }
}

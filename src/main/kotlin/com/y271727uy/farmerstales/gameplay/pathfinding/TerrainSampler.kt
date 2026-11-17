package com.y271727uy.farmerstales.gameplay.pathfinding

import com.y271727uy.computatio.algorithm.fastpath.model.GridPos
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalBounds
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalCell
import com.y271727uy.computatio.algorithm.fastpath.model.TraversalSnapshot
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.FluidTags
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.Mob
import net.minecraft.world.level.pathfinder.BlockPathTypes
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator
import java.util.WeakHashMap
import kotlin.math.max
import kotlin.math.min

/**
 * Samples loaded terrain into Computatio [TraversalSnapshot]s on the server thread.
 *
 * Cells follow vanilla's ground model: a cell is the voxel holding a mob's feet, its floor height comes
 * from [WalkNodeEvaluator.getFloorLevel], and its cost is the mob's own pathfinding malus, so danger
 * blocks, closed doors and fences are avoided exactly as vanilla would.
 *
 * Snapshots are snapped to a coarse grid and cached for a short time per (level, entity type, body
 * height). All sheep in a flock, or all wolves in a pack, therefore usually share one sample instead of
 * each re-reading the same blocks.
 */
internal object TerrainSampler {
    private const val HORIZONTAL_PADDING = 6
    private const val VERTICAL_PADDING = 3
    private const val MAX_RADIUS = 24
    private const val HORIZONTAL_SNAP = 8
    private const val VERTICAL_SNAP = 4
    private const val CACHE_TTL_TICKS = 20L
    private const val MAX_CACHED_PER_LEVEL = 32

    private val cache = WeakHashMap<ServerLevel, ArrayDeque<CachedSnapshot>>()

    fun worldId(level: ServerLevel): String = level.dimension().location().toString()

    /** Returns a snapshot covering [start]..[target] for [mob], or null when terrain is unavailable. */
    fun snapshotFor(level: ServerLevel, mob: Mob, start: GridPos, target: GridPos): TraversalSnapshot? {
        val profile = Profile(mob.type, Mth.ceil(mob.bbHeight).coerceAtLeast(1))
        val required = TraversalBounds.forPath(start, target, MAX_RADIUS, level.minBuildHeight + 1, level.maxBuildHeight - 2)
            .let { // forPath pads generously; tighten to what a short livestock route needs.
                TraversalBounds(
                    max(it.minX, min(start.x(), target.x()) - HORIZONTAL_PADDING),
                    min(it.maxX, max(start.x(), target.x()) + HORIZONTAL_PADDING),
                    max(it.minZ, min(start.z(), target.z()) - HORIZONTAL_PADDING),
                    min(it.maxZ, max(start.z(), target.z()) + HORIZONTAL_PADDING),
                    max(it.minY, min(start.y(), target.y()) - VERTICAL_PADDING),
                    min(it.maxY, max(start.y(), target.y()) + VERTICAL_PADDING),
                )
            }
        val now = level.gameTime
        val entries = cache.getOrPut(level) { ArrayDeque() }
        entries.removeAll { now - it.capturedAt > CACHE_TTL_TICKS }
        entries.firstOrNull { it.profile == profile && it.bounds.contains(required) }?.let { return it.snapshot }

        val bounds = snap(required, level)
        val snapshot = sample(level, mob, profile, bounds, start, target) ?: return null
        entries.addLast(CachedSnapshot(profile, bounds, now, snapshot))
        while (entries.size > MAX_CACHED_PER_LEVEL) entries.removeFirst()
        return snapshot
    }

    fun clear() = cache.clear()

    private fun snap(bounds: TraversalBounds, level: ServerLevel): TraversalBounds = TraversalBounds(
        Math.floorDiv(bounds.minX(), HORIZONTAL_SNAP) * HORIZONTAL_SNAP,
        Math.floorDiv(bounds.maxX(), HORIZONTAL_SNAP) * HORIZONTAL_SNAP + HORIZONTAL_SNAP - 1,
        Math.floorDiv(bounds.minZ(), HORIZONTAL_SNAP) * HORIZONTAL_SNAP,
        Math.floorDiv(bounds.maxZ(), HORIZONTAL_SNAP) * HORIZONTAL_SNAP + HORIZONTAL_SNAP - 1,
        max(level.minBuildHeight + 1, Math.floorDiv(bounds.minY(), VERTICAL_SNAP) * VERTICAL_SNAP),
        min(level.maxBuildHeight - 2, Math.floorDiv(bounds.maxY(), VERTICAL_SNAP) * VERTICAL_SNAP + VERTICAL_SNAP - 1),
    )

    @Suppress("DEPRECATION") // Level.hasChunksAt is still vanilla's loaded-area check in 1.20.1.
    private fun sample(
        level: ServerLevel,
        mob: Mob,
        profile: Profile,
        bounds: TraversalBounds,
        start: GridPos,
        target: GridPos,
    ): TraversalSnapshot? {
        val minX = bounds.minX()
        val minZ = bounds.minZ()
        // One extra layer below for floors and enough above for the body standing on the top layer.
        val sampleMinY = bounds.minY() - 1
        val sampleMaxY = bounds.maxY() + profile.bodyHeight
        if (!level.hasChunksAt(BlockPos(minX, sampleMinY, minZ), BlockPos(bounds.maxX(), sampleMaxY, bounds.maxZ()))) {
            return null
        }
        val sizeX = bounds.maxX() - minX + 1
        val sizeZ = bounds.maxZ() - minZ + 1
        val sizeY = sampleMaxY - sampleMinY + 1
        // 0 = solid, 1 = open air/passable, 2 = water.
        val voxels = ByteArray(sizeX * sizeY * sizeZ)
        fun index(x: Int, y: Int, z: Int) = ((x - minX) * sizeZ + (z - minZ)) * sizeY + (y - sampleMinY)

        val pos = BlockPos.MutableBlockPos()
        for (x in minX..bounds.maxX()) for (z in minZ..bounds.maxZ()) for (y in sampleMinY..sampleMaxY) {
            pos.set(x, y, z)
            val state = level.getBlockState(pos)
            val fluid = state.fluidState
            // Solidity first: waterlogged fences, slabs and stairs carry a water fluid state but still block.
            voxels[index(x, y, z)] = when {
                fluid.`is`(FluidTags.LAVA) -> SOLID
                !state.isPathfindable(level, pos, PathComputationType.LAND) -> SOLID
                fluid.`is`(FluidTags.WATER) -> WATER
                else -> OPEN
            }
        }

        val builder = TraversalSnapshot.builder(
            worldId(level), start, target, bounds.minY(), bounds.maxY(), level.gameTime, true,
        )
        val typePos = BlockPos.MutableBlockPos()
        for (x in minX..bounds.maxX()) for (z in minZ..bounds.maxZ()) for (y in bounds.minY()..bounds.maxY()) {
            val here = voxels[index(x, y, z)]
            if (here == SOLID) continue
            builder.addBodyPassage(x, y, z)

            val below = voxels[index(x, y - 1, z)]
            val water = here == WATER
            if (water) {
                // Land mobs float, so only the surface layer of a water column is a useful cell.
                if (voxels[index(x, y + 1, z)] == WATER) continue
            } else if (below != SOLID) {
                continue
            }
            var headroom = true
            for (dy in 1 until profile.bodyHeight) {
                if (voxels[index(x, y + dy, z)] == SOLID) {
                    headroom = false
                    break
                }
            }
            if (!headroom) continue

            typePos.set(x, y, z)
            val type = WalkNodeEvaluator.getBlockPathTypeStatic(level, typePos)
            if (type == BlockPathTypes.OPEN) continue
            val malus = mob.getPathfindingMalus(type)
            if (malus < 0.0f) continue
            val standY = if (water) y.toDouble() else WalkNodeEvaluator.getFloorLevel(level, pos.set(x, y, z))
            // Floors of normal blocks never rise above the feet voxel; only 1.5-tall fences/walls do.
            if (standY > y + FLOOR_EPSILON) continue
            builder.addCell(TraversalCell(x, y, z, standY, water, false, false, 1.0 + malus))
        }
        return builder.build()
    }

    private const val FLOOR_EPSILON = 1.0E-3
    private const val SOLID: Byte = 0
    private const val OPEN: Byte = 1
    private const val WATER: Byte = 2

    private data class Profile(val type: EntityType<*>, val bodyHeight: Int)

    private class CachedSnapshot(
        val profile: Profile,
        val bounds: TraversalBounds,
        val capturedAt: Long,
        val snapshot: TraversalSnapshot,
    )
}

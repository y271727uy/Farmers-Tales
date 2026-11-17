package com.y271727uy.farmerstales.gameplay.pathfinding

import com.mojang.logging.LogUtils
import com.y271727uy.computatio.algorithm.fastpath.model.GridPos
import com.y271727uy.computatio.algorithm.fastpath.model.MovementMode
import com.y271727uy.computatio.algorithm.fastpath.model.PathCapabilities
import com.y271727uy.computatio.algorithm.fastpath.model.PathIntent
import com.y271727uy.computatio.algorithm.fastpath.model.PathRequest
import com.y271727uy.computatio.algorithm.fastpath.model.PathResult
import com.y271727uy.computatio.algorithm.fastpath.model.PathVector
import com.y271727uy.computatio.api.fastpath.FastPath
import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation
import net.minecraft.world.level.pathfinder.BlockPathTypes
import net.minecraft.world.level.pathfinder.Node
import net.minecraft.world.level.pathfinder.Path
import net.minecraftforge.event.server.ServerStoppingEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * Drop-in replacement for `mob.navigation.moveTo(...)` in per-tick AI code, backed by Computatio.
 *
 * - Repeated requests to (almost) the same target while a route is still being followed only update
 *   the speed, so goals may keep calling this every tick without re-running A*.
 * - Terrain is sampled once on the server thread into a shared, cached [TerrainSampler] snapshot; the
 *   A* search itself runs on a worker pool via [FastPath.findAsync].
 * - The resulting route is handed to vanilla navigation as a regular [Path], so movement, jumping,
 *   swimming and stuck detection stay vanilla.
 * - Anything Computatio cannot serve (non-ground navigation, wide mobs, far targets, unloaded terrain,
 *   no route) falls back to vanilla `moveTo`, still with request de-duplication.
 *
 * Callers must use [stop] instead of `navigation.stop()` so a search still in flight is discarded.
 * All methods must be called on the server thread.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object FastPathNavigation {
    private val LOGGER = LogUtils.getLogger()

    private const val REUSE_DISTANCE_SQR = 1.5 * 1.5
    private const val MIN_REPLAN_TICKS = 10L
    private const val MAX_PENDING_TICKS = 10L
    private const val MAX_FAST_PATH_DISTANCE = 24
    private const val MAX_BODY_WIDTH = 1.0f
    private val GROUND_ONLY = PathCapabilities(false, false, false, false)

    private val executor = Executors.newFixedThreadPool(2, object : ThreadFactory {
        private val counter = AtomicInteger()
        override fun newThread(task: Runnable) = Thread(task, "FarmersTales-FastPath-${counter.incrementAndGet()}").apply {
            isDaemon = true
            priority = Thread.NORM_PRIORITY - 1
        }
    })

    private val states = WeakHashMap<Mob, NavState>()

    fun moveTo(mob: Mob, target: Entity, speed: Double) = moveTo(mob, target.x, target.y, target.z, speed)

    fun moveTo(mob: Mob, x: Double, y: Double, z: Double, speed: Double) {
        val level = mob.level() as? ServerLevel ?: run {
            mob.navigation.moveTo(x, y, z, speed)
            return
        }
        val now = level.gameTime
        val previous = states[mob]
        if (previous != null && previous.isSameTarget(x, y, z)) {
            if (previous.pending) {
                previous.speed = speed
                return
            }
            val following = mob.navigation.path === previous.path && !mob.navigation.isDone
            if (following || now - previous.issuedAt < MIN_REPLAN_TICKS) {
                previous.speed = speed
                if (following) mob.navigation.setSpeedModifier(speed)
                return
            }
        }

        val state = NavState(x, y, z, speed, now)
        states[mob] = state
        if (!submitSearch(level, mob, state)) moveVanilla(mob, state)
    }

    /** Stops the mob and discards any search still in flight. */
    fun stop(mob: Mob) {
        states.remove(mob)
        mob.navigation.stop()
    }

    private fun submitSearch(level: ServerLevel, mob: Mob, state: NavState): Boolean {
        if (mob.navigation !is GroundPathNavigation || mob.bbWidth > MAX_BODY_WIDTH || mob.isNoGravity) return false
        val start = mob.blockPosition()
        val target = BlockPos.containing(state.x, state.y, state.z)
        if (abs(start.x - target.x) > MAX_FAST_PATH_DISTANCE || abs(start.z - target.z) > MAX_FAST_PATH_DISTANCE) return false

        val startGrid = GridPos(start.x, start.y, start.z)
        val targetGrid = GridPos(target.x, target.y, target.z)
        val snapshot = try {
            TerrainSampler.snapshotFor(level, mob, startGrid, targetGrid)
        } catch (exception: RuntimeException) {
            LOGGER.debug("FastPath terrain sampling failed for {}", mob, exception)
            null
        } ?: return false

        val request = PathRequest(
            TerrainSampler.worldId(level),
            startGrid,
            PathVector(target.x + 0.5, target.y.toDouble(), target.z + 0.5),
            PathIntent.WALK,
            GROUND_ONLY,
        )
        state.pending = true
        val server = level.server
        FastPath.findAsync(request, snapshot, executor).whenComplete { result, error ->
            server.execute { applyResult(mob, level, state, result, error) }
        }
        return true
    }

    private fun applyResult(mob: Mob, level: ServerLevel, state: NavState, result: PathResult?, error: Throwable?) {
        if (states[mob] !== state) return // superseded or stopped
        state.pending = false
        if (mob.isRemoved || mob.level() !== level) {
            states.remove(mob)
            return
        }
        if (error != null) LOGGER.debug("FastPath search failed for {}", mob, error)
        val path = if (error == null && level.gameTime - state.issuedAt <= MAX_PENDING_TICKS) result?.let(::toVanillaPath) else null
        if (path != null && mob.navigation.moveTo(path, state.speed)) {
            state.path = mob.navigation.path
        } else {
            moveVanilla(mob, state)
        }
    }

    private fun moveVanilla(mob: Mob, state: NavState) {
        state.pending = false
        mob.navigation.moveTo(state.x, state.y, state.z, state.speed)
        state.path = mob.navigation.path
    }

    private fun toVanillaPath(result: PathResult): Path? {
        if (!result.success() || result.waypoints().isEmpty()) return null
        val nodes = result.waypoints().map { waypoint ->
            val cell = waypoint.blockPosition()
            Node(cell.x(), cell.y(), cell.z()).apply {
                type = if (waypoint.mode() == MovementMode.SWIM) BlockPathTypes.WATER else BlockPathTypes.WALKABLE
            }
        }
        val end = nodes.last()
        return Path(nodes, BlockPos(end.x, end.y, end.z), result.status() == PathResult.Status.EXACT_TARGET)
    }

    @JvmStatic
    @SubscribeEvent
    fun onServerStopping(@Suppress("UNUSED_PARAMETER") event: ServerStoppingEvent) {
        states.clear()
        TerrainSampler.clear()
    }

    private class NavState(val x: Double, val y: Double, val z: Double, var speed: Double, val issuedAt: Long) {
        @Volatile
        var pending = false
        var path: Path? = null

        fun isSameTarget(otherX: Double, otherY: Double, otherZ: Double): Boolean {
            val dx = x - otherX
            val dy = y - otherY
            val dz = z - otherZ
            return dx * dx + dy * dy + dz * dz <= REUSE_DISTANCE_SQR
        }
    }
}

package com.y271727uy.farmerstales.gameplay.animal_husbandry.raid

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.Wolf
import net.minecraft.world.item.InstrumentItem
import net.minecraft.world.level.block.CampfireBlock
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap
import kotlin.math.hypot

/** Fire and horn keep raid wolves off livestock without cancelling the raid. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object RaidWolfDeterrence {
    private val hornScares = WeakHashMap<Wolf, HornScare>()
    private val campfireCache = WeakHashMap<Wolf, CampfireCache>()

    @JvmStatic
    fun shouldFlee(wolf: Wolf): Boolean =
        isRaidWolf(wolf) && (hornScare(wolf) != null || litCampfireNear(wolf) != null)

    @JvmStatic
    fun fleeOrigin(wolf: Wolf): Vec3? =
        hornScare(wolf)?.origin ?: litCampfireNear(wolf)

    @JvmStatic
    @SubscribeEvent
    fun onUseHorn(event: LivingEntityUseItemEvent.Start) {
        val player = event.entity as? ServerPlayer ?: return
        if (event.item.item !is InstrumentItem) return
        scareWolvesFrom(player)
    }

    private fun scareWolvesFrom(player: ServerPlayer) {
        val level = player.serverLevel()
        val origin = player.position()
        val until = level.gameTime + HORN_SCARE_TICKS
        level.getEntitiesOfClass(
            Wolf::class.java,
            player.boundingBox.inflate(HORN_RANGE),
        ) { wolf -> isRaidWolf(wolf) && wolf.distanceToSqr(player) <= HORN_RANGE_SQR }
            .forEach { wolf ->
                wolf.target = null
                wolf.lastHurtByMob = null
                hornScares[wolf] = HornScare(until, origin)
            }
    }

    private fun hornScare(wolf: Wolf): HornScare? {
        val scare = hornScares[wolf] ?: return null
        val level = wolf.level() as? ServerLevel ?: return null
        if (scare.until <= level.gameTime) {
            hornScares.remove(wolf)
            return null
        }
        return scare
    }

    private fun litCampfireNear(wolf: Wolf): Vec3? {
        val level = wolf.level() as? ServerLevel ?: return null
        val cached = campfireCache[wolf]
        if (cached != null && cached.expiresAt > level.gameTime) {
            return cached.origin
        }

        val origin = findLitCampfire(wolf)
        campfireCache[wolf] = CampfireCache(level.gameTime + CAMPFIRE_SCAN_INTERVAL, origin)
        return origin
    }

    private fun findLitCampfire(wolf: Wolf): Vec3? {
        val level = wolf.level()
        val center = wolf.blockPosition()
        var nearest: BlockPos? = null
        var nearestDist = Double.MAX_VALUE
        for (dx in -CAMPFIRE_RADIUS..CAMPFIRE_RADIUS) {
            for (dz in -CAMPFIRE_RADIUS..CAMPFIRE_RADIUS) {
                for (dy in -CAMPFIRE_VERTICAL..CAMPFIRE_VERTICAL) {
                    val pos = center.offset(dx, dy, dz)
                    if (!CampfireBlock.isLitCampfire(level.getBlockState(pos))) continue
                    val dist = hypot(wolf.x - (pos.x + 0.5), wolf.z - (pos.z + 0.5))
                    if (dist <= CAMPFIRE_RADIUS && dist < nearestDist) {
                        nearest = pos
                        nearestDist = dist
                    }
                }
            }
        }
        return nearest?.let { Vec3(it.x + 0.5, it.y.toDouble(), it.z + 0.5) }
    }

    private fun isRaidWolf(wolf: Wolf): Boolean =
        !wolf.level().isClientSide && wolf.isAlive && !wolf.isTame && LivestockRaiderState.isRaider(wolf)

    private const val HORN_RANGE = 10.0
    private const val HORN_RANGE_SQR = HORN_RANGE * HORN_RANGE
    private const val HORN_SCARE_TICKS = 100
    private const val CAMPFIRE_RADIUS = 4
    private const val CAMPFIRE_VERTICAL = 2
    private const val CAMPFIRE_SCAN_INTERVAL = 10L

    private class HornScare(val until: Long, val origin: Vec3)

    private class CampfireCache(val expiresAt: Long, val origin: Vec3?)
}

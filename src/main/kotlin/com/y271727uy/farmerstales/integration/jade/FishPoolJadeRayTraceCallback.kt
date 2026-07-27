package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import snownee.jade.api.Accessor
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.callback.JadeRayTraceCallback

/** Expands only Jade's visual targeting area for fish pools. */
class FishPoolJadeRayTraceCallback(
    private val registration: IWailaClientRegistration,
) : JadeRayTraceCallback {
    override fun onRayTrace(
        hitResult: HitResult,
        blockAccessor: Accessor<*>?,
        entityAccessor: Accessor<*>?,
    ): Accessor<*>? {
        val originalAccessor = entityAccessor ?: blockAccessor
        if (entityAccessor != null || hitResult.type == HitResult.Type.ENTITY) return originalAccessor

        val player = Minecraft.getInstance().player ?: return originalAccessor
        val start = player.eyePosition
        val end = hitResult.location
        if (start == end) return originalAccessor

        val searchBounds = AABB(start, end).inflate(
            HORIZONTAL_EXPANSION,
            VERTICAL_EXPANSION - VISUAL_Y_OFFSET,
            HORIZONTAL_EXPANSION,
        )
        var nearestHit: EntityHitResult? = null
        var nearestDistance = Double.MAX_VALUE

        for (entity in player.level().getEntities(player, searchBounds) { it is AbstractFishPoolEntity && it.isPickable }) {
            val hitLocation = entity.boundingBox
                .move(0.0, VISUAL_Y_OFFSET, 0.0)
                .inflate(HORIZONTAL_EXPANSION, VERTICAL_EXPANSION, HORIZONTAL_EXPANSION)
                .clip(start, end)
                .orElse(null)
                ?: continue
            val distance = start.distanceToSqr(hitLocation)
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearestHit = EntityHitResult(entity, hitLocation)
            }
        }

        return nearestHit?.let { registration.entityAccessor().hit(it).entity(it.entity).build() } ?: originalAccessor
    }

    private companion object {
        const val HORIZONTAL_EXPANSION = 1.5
        const val VERTICAL_EXPANSION = 1.0
        // Keep this aligned with the fish-pool renderers' vertical translation.
        const val VISUAL_Y_OFFSET = -3.5
    }
}

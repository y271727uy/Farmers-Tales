package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity

/**
 * Quality for harvests that only surface as a freshly spawned item entity.
 * The block never tells us who picked it, so the nearest player is the harvester.
 */
internal object FreshHarvestDrop {
    private const val HARVESTER_RANGE = 8.0

    /** Callers have already decided this entity is a harvest product without a quality. */
    fun applyQuality(level: ServerLevel, itemEntity: ItemEntity) {
        val harvester = level.getNearestPlayer(itemEntity, HARVESTER_RANGE)
        if (!QualityHarvestRules.canReceiveQuality(harvester)) return
        if (QualityDropProcessor.rollInto(itemEntity.item, harvester, level.random)) {
            FarmersTalesAdvancements.awardIfRaining(harvester, level)
        }
    }
}

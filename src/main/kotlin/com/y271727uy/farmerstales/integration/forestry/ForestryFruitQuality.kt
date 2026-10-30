package com.y271727uy.farmerstales.integration.forestry

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.harvestPickupDelay
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.integration.IntegrationManager
import forestry.core.items.ItemFruit
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod

/**
 * 林业果实由基因组决定，不能按物品 id 列白名单。
 * FRMC 把果叶右键收获改成了掉落物；果荚破坏也掉同一种 [ItemFruit]。
 * 苹果和可可等位基因产出的是原版物品，不在这里打品质。
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object ForestryFruitQuality {

    @JvmStatic
    @SubscribeEvent
    fun onItemJoinLevel(event: EntityJoinLevelEvent) {
        if (!ModList.get().isLoaded(IntegrationManager.FORESTRY)) {
            return
        }

        val level = event.level
        if (level.isClientSide || level !is ServerLevel) {
            return
        }

        val itemEntity = event.entity as? ItemEntity ?: return
        if (itemEntity.harvestPickupDelay() < FRESH_PICKUP_DELAY) {
            return
        }

        val stack = itemEntity.item
        if (stack.isEmpty || QualityNbt.hasValidQuality(stack) || !ForestryFruitDrops.isHarvest(stack)) {
            return
        }

        val harvester = level.getNearestPlayer(itemEntity, HARVESTER_RANGE)
        if (!QualityHarvestRules.canReceiveQuality(harvester)) {
            return
        }

        QualityRoller.roll(level.random)?.let { quality ->
            QualityNbt.write(stack, quality, harvester)
            FarmersTalesAdvancements.awardIfRaining(harvester, level)
        }
    }

    private const val FRESH_PICKUP_DELAY = 10
    private const val HARVESTER_RANGE = 8.0
}

/** Loaded only after Forestry is confirmed present. */
internal object ForestryFruitDrops {
    fun isHarvest(stack: ItemStack): Boolean = stack.item is ItemFruit
}

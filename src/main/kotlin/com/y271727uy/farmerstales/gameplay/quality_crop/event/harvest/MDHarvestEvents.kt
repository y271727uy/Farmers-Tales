package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.harvestPickupDelay
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.quality_crop.util.MDWhiteList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * 庄园馀话的果实由方块自己生成掉落物。
 * FRMC 还覆盖了果树的右键收获，所以这里不再重做收获，只给新生成的庄园产物打品质。
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object MDHarvestEvents {

	@JvmStatic
	@SubscribeEvent
	fun onItemJoinLevel(event: EntityJoinLevelEvent) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		val itemEntity = event.entity as? ItemEntity ?: return
		if (itemEntity.harvestPickupDelay() < FRESH_PICKUP_DELAY) {
			return
		}

		val stack = itemEntity.item
		if (stack.isEmpty || QualityNbt.hasValidQuality(stack) || !MDWhiteList.matches(stack)) {
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

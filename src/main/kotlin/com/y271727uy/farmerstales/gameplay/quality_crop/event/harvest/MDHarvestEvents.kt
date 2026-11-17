package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.quality_crop.harvestPickupDelay
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
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

		FreshHarvestDrop.applyQuality(level, itemEntity)
	}

	private const val FRESH_PICKUP_DELAY = 10
}

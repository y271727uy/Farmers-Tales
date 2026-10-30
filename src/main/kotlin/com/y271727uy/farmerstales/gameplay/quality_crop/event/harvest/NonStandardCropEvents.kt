package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityDropProcessor
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.util.NonStandardCropWhitelist
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
@Suppress("unused")
object NonStandardCropEvents {

	@JvmStatic
	@SubscribeEvent
	fun onBlockBreak(event: BlockEvent.BreakEvent) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		val state = level.getBlockState(event.pos)
		if (!NonStandardCropWhitelist.matches(state.block)) {
			return
		}
		val shouldApplyQuality = QualityHarvestRules.canReceiveQuality(event.player)

		val drops = Block.getDrops(state, level, event.pos, level.getBlockEntity(event.pos))
		if (drops.isEmpty()) {
			return
		}

		event.isCanceled = true
		level.removeBlock(event.pos, false)
		FarmersTalesAdvancements.awardIfRaining(event.player, level)

		for (stack in drops) {
			if (stack.isEmpty) {
				continue
			}

			if (shouldApplyQuality) {
				QualityDropProcessor.applyNonStandard(stack, state.block, event.player, level.random)
			}

			Block.popResource(level, event.pos, stack)
		}
	}

}




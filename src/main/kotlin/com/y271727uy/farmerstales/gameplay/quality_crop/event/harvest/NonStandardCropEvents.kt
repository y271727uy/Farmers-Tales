package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.util.NonStandardCropWhitelist
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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

			if (QualityNbt.hasValidQuality(stack) || !NonStandardCropWhitelist.matches(stack, state.block)) {
				Block.popResource(level, event.pos, stack)
				continue
			}

			if (shouldApplyQuality) {
				QualityRoller.roll(level.random)?.let { quality ->
					QualityNbt.write(stack, quality, event.player)
				}
			}

			Block.popResource(level, event.pos, stack)
		}
	}

}




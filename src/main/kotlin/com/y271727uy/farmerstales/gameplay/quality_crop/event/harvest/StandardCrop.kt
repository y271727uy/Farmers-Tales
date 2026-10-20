package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.util.StandardCropWhitelist
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.CropBlock
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object StandardCrop {

	@JvmStatic
	@SubscribeEvent
	fun onBlockBreak(event: BlockEvent.BreakEvent) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		val state = level.getBlockState(event.pos)
		val block = state.block
		if (block !is CropBlock || !block.isMaxAge(state)) {
			return
		}

		event.isCanceled = true
		val drops = getHarvestDrops(level, event.pos, state, QualityHarvestRules.canReceiveQuality(event.player), event.player)
		level.removeBlock(event.pos, false)

		for (stack in drops) {
			if (stack.isEmpty) {
				continue
			}
			Block.popResource(level, event.pos, stack)
		}
	}

	/** Returns mature-crop loot after applying this mod's crop quality rules. */
	fun getHarvestDrops(
		level: ServerLevel,
		pos: net.minecraft.core.BlockPos,
		state: net.minecraft.world.level.block.state.BlockState,
		shouldApplyQuality: Boolean,
		harvester: net.minecraft.world.entity.player.Player? = null,
	) =
		Block.getDrops(state, level, pos, level.getBlockEntity(pos)).onEach { stack ->
			FarmersTalesAdvancements.awardIfRaining(harvester, level)
			if (shouldApplyQuality && !stack.isEmpty && !QualityNbt.hasValidQuality(stack) && StandardCropWhitelist.matches(stack)) {
				QualityRoller.roll(level.random)?.let { quality -> QualityNbt.write(stack, quality, harvester) }
			}
		}
}

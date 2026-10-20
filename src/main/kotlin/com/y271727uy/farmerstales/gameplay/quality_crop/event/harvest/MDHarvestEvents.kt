package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.util.MDWhiteList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object MDHarvestEvents {

	@JvmStatic
	@SubscribeEvent
	fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		if (event.hand != InteractionHand.MAIN_HAND) {
			return
		}

		val state = level.getBlockState(event.pos)
		if (isManorsBountyFruitLeaf(state)) {
			return
		}
		if (!MDWhiteList.matches(state.block)) {
			return
		}
		val shouldApplyQuality = QualityHarvestRules.canReceiveQuality(event.entity)

		if (!isHarvestReady(state)) {
			return
		}

		val drops = Block.getDrops(state, level, event.pos, level.getBlockEntity(event.pos))
		val expectedDropId = MDWhiteList.expectedDropId(state.block)
		val finalDrops = if (expectedDropId == null) {
			drops
		} else {
			val hasExpectedDrop = drops.any { stack -> ForgeRegistries.ITEMS.getKey(stack.item) == expectedDropId }
			if (hasExpectedDrop) {
				drops
			} else {
				val fallbackItem = ForgeRegistries.ITEMS.getValue(expectedDropId) ?: return
				if (drops.isEmpty()) listOf(ItemStack(fallbackItem)) else drops + ItemStack(fallbackItem)
			}
		}
		if (finalDrops.isEmpty()) {
			return
		}

		event.setCancellationResult(InteractionResult.SUCCESS)
		event.isCanceled = true

		level.setBlockAndUpdate(event.pos, resetHarvestState(state))
		FarmersTalesAdvancements.awardIfRaining(event.entity, level)

		for (stack in finalDrops) {
			if (stack.isEmpty) {
				continue
			}

			if (QualityNbt.hasValidQuality(stack) || !MDWhiteList.matches(stack, state.block)) {
				Block.popResource(level, event.pos, stack)
				continue
			}

			if (shouldApplyQuality) {
				QualityRoller.roll(
					level.random,
					Config.mdIronCropQualityChance,
					Config.mdGoldCropQualityChance,
					Config.mdDiamondCropQualityChance,
				)?.let { quality ->
					QualityNbt.write(stack, quality, event.entity)
				}
			}

			Block.popResource(level, event.pos, stack)
		}
	}

	private fun isManorsBountyFruitLeaf(state: BlockState): Boolean {
		val blockId = ForgeRegistries.BLOCKS.getKey(state.block) ?: return false
		return blockId.namespace == "manors_bounty" && state.properties.any { it.name == "can_fruit" }
	}

	private fun isHarvestReady(state: BlockState): Boolean {
		var canFruit = false
		var ageReady = false

		for (property in state.properties) {
			when (property) {
				is IntegerProperty -> {
					if (property.name == "age") {
						val maxAge = property.possibleValues.maxOrNull() ?: continue
						ageReady = state.getValue(property) >= maxAge
					}
				}
				else -> Unit
			}

			if (property.name == "can_fruit") {
				canFruit = currentValue(state, property).equals("TRUE", ignoreCase = true)
			}
		}

		return canFruit && ageReady
	}

	private fun resetHarvestState(state: BlockState): BlockState {
		for (property in state.properties) {
			when (property) {
				is IntegerProperty -> {
					if (property.name == "age") {
						val resetValue = when {
							1 in property.possibleValues -> 1
							property.possibleValues.isNotEmpty() -> property.possibleValues.minOrNull() ?: state.getValue(property)
							else -> state.getValue(property)
						}
						if (resetValue != state.getValue(property)) {
							return state.setValue(property, resetValue)
						}
					}
				}
				else -> Unit
			}
		}

		return state
	}

	@Suppress("UNCHECKED_CAST")
	private fun currentValue(state: BlockState, property: Property<*>): String {
		val typed = property as Property<Comparable<Any>>
		return state.getValue(typed).toString()
	}
}




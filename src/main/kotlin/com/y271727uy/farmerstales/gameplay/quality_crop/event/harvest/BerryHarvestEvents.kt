package com.y271727uy.farmerstales.gameplay.quality_crop.event.harvest

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityHarvestRules
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityNbt
import com.y271727uy.farmerstales.gameplay.quality_crop.QualityRoller
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.quality_crop.util.BerryFruitHarvestWhitelist
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object BerryHarvestEvents {

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
		if (!BerryFruitHarvestWhitelist.matches(state.block)) {
			return
		}
		val shouldApplyQuality = QualityHarvestRules.canReceiveQuality(event.entity)

		if (!isHarvestReady(state)) {
			return
		}

		val drops = Block.getDrops(state, level, event.pos, level.getBlockEntity(event.pos))
		if (drops.isEmpty()) {
			return
		}

		event.setCancellationResult(InteractionResult.SUCCESS)
		event.isCanceled = true

		level.setBlockAndUpdate(event.pos, resetHarvestState(state))
		FarmersTalesAdvancements.awardIfRaining(event.entity, level)

		for (stack in drops) {
			if (stack.isEmpty) {
				continue
			}

			if (QualityNbt.hasValidQuality(stack) || !BerryFruitHarvestWhitelist.matches(stack, state.block)) {
				Block.popResource(level, event.pos, stack)
				continue
			}

			if (shouldApplyQuality) {
				QualityRoller.roll(level.random)?.let { quality ->
					QualityNbt.write(stack, quality, event.entity)
				}
			}

			Block.popResource(level, event.pos, stack)
		}
	}

	private fun isHarvestReady(state: BlockState): Boolean {
		for (property in state.properties) {
			when (property) {
				is IntegerProperty -> {
					if (property.name == "age") {
						val maxAge = property.possibleValues.maxOrNull() ?: continue
						if (state.getValue(property) >= maxAge) {
							return true
						}
					}
				}
				is BooleanProperty -> {
					if (property.name == "berries" && state.getValue(property)) {
						return true
					}
				}
				else -> {
					val value = currentValue(state, property).uppercase()
					if (property.name == "type" && value in HARVEST_READY_VALUES) {
						return true
					}
				}
			}
		}

		return false
	}

	private fun resetHarvestState(state: BlockState): BlockState {
		for (property in state.properties) {
			when (property) {
				is BooleanProperty -> {
					if (property.name == "berries" && state.getValue(property)) {
						return state.setValue(property, false)
					}
				}
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
				else -> {
					if (property.name == "type") {
						val resetValue = property.possibleValues.firstOrNull { candidate ->
							candidate.toString().uppercase() !in HARVEST_READY_VALUES
						} ?: property.possibleValues.firstOrNull { candidate ->
							candidate.toString().equals("LEAVES", ignoreCase = true) || candidate.toString().equals("FLOWERS", ignoreCase = true)
						}

						if (resetValue != null) {
							@Suppress("UNCHECKED_CAST")
							val typed = property as Property<Comparable<Any>>
							@Suppress("UNCHECKED_CAST")
							val typedValue = resetValue as Comparable<Any>
							if (currentValue(state, property).uppercase() in HARVEST_READY_VALUES) {
								return state.setValue(typed, typedValue)
							}
						}
					}
				}
			}
		}

		return state
	}

	@Suppress("UNCHECKED_CAST")
	private fun currentValue(state: BlockState, property: Property<*>): String {
		val typed = property as Property<Comparable<Any>>
		return state.getValue(typed).toString()
	}

	private val HARVEST_READY_VALUES = setOf("FRUITS", "FRUIT", "BERRIES", "RIPE", "MATURE", "READY")
}




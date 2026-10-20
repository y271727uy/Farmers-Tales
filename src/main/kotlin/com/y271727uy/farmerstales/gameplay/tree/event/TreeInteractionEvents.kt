package com.y271727uy.farmerstales.gameplay.tree.event

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeStumpBlockEntity
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.AxeItem
import net.minecraft.world.item.Items
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object TreeInteractionEvents {
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

		if (level.getBlockState(event.pos).block != ModBlocks.TREE_STUMP.get()) {
			return
		}

		val blockEntity = level.getBlockEntity(event.pos) as? TreeStumpBlockEntity ?: return
		if (!blockEntity.validateAndNormalize()) {
			return
		}

		val stack = event.itemStack
		if (stack.isEmpty) {
			return
		}

		val handled = when {
			stack.item == Items.BONE_MEAL -> blockEntity.applyGrowthCatalyst(event.entity, stack)
			stack.item == Items.POTION -> blockEntity.applyPotionWatering(event.entity, stack)
			stack.item is AxeItem -> blockEntity.harvestBranches(event.entity)
			else -> false
		}

		if (!handled) {
			return
		}

		if (blockEntity.maintenanceScore() >= 100) {
			FarmersTalesAdvancements.award(event.entity as net.minecraft.server.level.ServerPlayer, "orchard/lush_branches")
		}

		event.setCancellationResult(InteractionResult.SUCCESS)
		event.setCanceled(true)
	}
}

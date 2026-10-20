package com.y271727uy.farmerstales.gameplay.tree.event

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.gameplay.tree.TreeDefinitions
import com.y271727uy.farmerstales.gameplay.advancements.FarmersTalesAdvancements
import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeCompostBlockEntity
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.item.BlockItem
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionResult
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@Suppress("unused")
object TreePlacementEvents {
	@JvmStatic
	@SubscribeEvent
	fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
		val level = event.level
		if (level.isClientSide || level !is ServerLevel) {
			return
		}

		val stack = event.itemStack
		if (!TreeDefinitions.isWhiteListedSeed(stack)) {
			return
		}

		val seedId = ForgeRegistries.ITEMS.getKey(stack.item)?.toString() ?: return

		if (event.face != Direction.UP) {
			return
		}

		val clickedState = level.getBlockState(event.pos)
		if (clickedState.block != ModBlocks.TREE_COMPOST.get()) {
			return
		}

		val placePos = event.pos.above()
		if (!level.getBlockState(placePos).isAir) {
			return
		}

		val definition = TreeDefinitions.definitionOf(stack) ?: return
		val blockItem = stack.item as? BlockItem ?: return
		val dy = level.random.nextInt(3) - 1

		event.setCancellationResult(InteractionResult.SUCCESS)
		event.setCanceled(true)

		if (!level.setBlock(placePos, blockItem.block.defaultBlockState(), 3)) {
			return
		}
		if (!event.entity.abilities.instabuild) {
			stack.shrink(1)
		}

		level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F)

		val blockEntity = level.getBlockEntity(event.pos) as? TreeCompostBlockEntity ?: return
		blockEntity.treeType = seedId
		blockEntity.treeShape = definition.shapeIndex
		blockEntity.dy = dy
		blockEntity.pendingGrowthTicks = -1
		TreeCompostBlockEntity.scheduleInitialGrowth(blockEntity, level.random)
		blockEntity.setChanged()
		(event.entity as? net.minecraft.server.level.ServerPlayer)?.let {
			FarmersTalesAdvancements.award(it, "orchard/plant_tree")
		}
	}
}

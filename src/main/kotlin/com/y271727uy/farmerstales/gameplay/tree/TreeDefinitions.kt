@file:Suppress("DEPRECATION", "unused")

package com.y271727uy.farmerstales.gameplay.tree

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BonemealableBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.tags.FluidTags
import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeStumpBlockEntity

data class TreeShape(
	val leaveRelativePos: List<BlockPos>,
	val logRelativePos: List<BlockPos>
)

data class TreeBlockStateSpec(
	val blockId: String,
	val properties: Map<String, String> = emptyMap()
)

data class TreeExtraPlacement(
	val state: TreeBlockStateSpec,
	val count: Int
)

data class TreeDefinition(
	val leafId: String,
	val logId: String,
	val shapeIndex: Int,
	val growingState: Int,
	val defaultState: Int,
	val ripeState: Int,
	val initialLeafState: Int? = null,
	val floweringLeafState: TreeBlockStateSpec? = null,
	val fruitLeafState: TreeBlockStateSpec? = null,
	val extraLeafPlacement: TreeExtraPlacement? = null,
	val exactLeafState: TreeBlockStateSpec? = null
) {
	fun baseLeafState(): Int {
		return initialLeafState ?: defaultState
	}
}

object TreeDefinitions {
	private const val ROOTED_DIRT = "minecraft:rooted_dirt"

	val shapes: List<TreeShape> = listOf(
		TreeShape(emptyList(), emptyList()),
		TreeShape(
			listOf(
				BlockPos(-2, 3, -2), BlockPos(-2, 3, -1), BlockPos(-2, 4, -1), BlockPos(-2, 3, 0), BlockPos(-2, 4, 0),
				BlockPos(-2, 3, 1), BlockPos(-2, 4, 1), BlockPos(-2, 3, 2), BlockPos(-1, 3, -2), BlockPos(-1, 4, -2),
				BlockPos(-1, 3, -1), BlockPos(-1, 4, -1), BlockPos(-1, 3, 0), BlockPos(-1, 4, 0), BlockPos(-1, 5, 0),
				BlockPos(-1, 6, 0), BlockPos(-1, 3, 1), BlockPos(-1, 4, 1), BlockPos(-1, 3, 2), BlockPos(-1, 4, 2),
				BlockPos(0, 3, -2), BlockPos(0, 4, -2), BlockPos(0, 3, -1), BlockPos(0, 4, -1), BlockPos(0, 5, -1),
				BlockPos(0, 6, -1), BlockPos(0, 5, 0), BlockPos(0, 6, 0), BlockPos(0, 3, 1), BlockPos(0, 4, 1),
				BlockPos(0, 5, 1), BlockPos(0, 6, 1), BlockPos(0, 3, 2), BlockPos(0, 4, 2), BlockPos(1, 3, -2),
				BlockPos(1, 4, -2), BlockPos(1, 3, -1), BlockPos(1, 4, -1), BlockPos(1, 3, 0), BlockPos(1, 4, 0),
				BlockPos(1, 5, 0), BlockPos(1, 6, 0), BlockPos(1, 3, 1), BlockPos(1, 4, 1), BlockPos(1, 3, 2),
				BlockPos(1, 4, 2), BlockPos(2, 3, -2), BlockPos(2, 3, -1), BlockPos(2, 4, -1), BlockPos(2, 3, 0),
				BlockPos(2, 4, 0), BlockPos(2, 3, 1), BlockPos(2, 4, 1), BlockPos(2, 3, 2)
			),
			listOf(BlockPos(0, 1, 0), BlockPos(0, 2, 0), BlockPos(0, 3, 0), BlockPos(0, 4, 0))
		),
		TreeShape(
			listOf(
				BlockPos(-3, 4, -1), BlockPos(-3, 4, 0), BlockPos(-3, 4, 1), BlockPos(-2, 4, -2), BlockPos(-2, 4, -1),
				BlockPos(-2, 5, -1), BlockPos(-2, 4, 0), BlockPos(-2, 5, 0), BlockPos(-2, 4, 1), BlockPos(-2, 5, 1),
				BlockPos(-2, 4, 2), BlockPos(-1, 4, -3), BlockPos(-1, 4, -2), BlockPos(-1, 5, -2), BlockPos(-1, 4, -1),
				BlockPos(-1, 5, -1), BlockPos(-1, 4, 0), BlockPos(-1, 5, 0), BlockPos(-1, 4, 1), BlockPos(-1, 5, 1),
				BlockPos(-1, 4, 2), BlockPos(-1, 5, 2), BlockPos(-1, 4, 3), BlockPos(0, 4, -3), BlockPos(0, 4, -2),
				BlockPos(0, 5, -2), BlockPos(0, 4, -1), BlockPos(0, 5, -1), BlockPos(0, 5, 0), BlockPos(0, 4, 1),
				BlockPos(0, 5, 1), BlockPos(0, 4, 2), BlockPos(0, 5, 2), BlockPos(0, 4, 3), BlockPos(1, 4, -3),
				BlockPos(1, 4, -2), BlockPos(1, 5, -2), BlockPos(1, 4, -1), BlockPos(1, 5, -1), BlockPos(1, 4, 0),
				BlockPos(1, 5, 0), BlockPos(1, 4, 1), BlockPos(1, 5, 1), BlockPos(1, 4, 2), BlockPos(1, 5, 2),
				BlockPos(1, 4, 3), BlockPos(2, 4, -2), BlockPos(2, 4, -1), BlockPos(2, 5, -1), BlockPos(2, 4, 0),
				BlockPos(2, 5, 0), BlockPos(2, 4, 1), BlockPos(2, 5, 1), BlockPos(2, 4, 2), BlockPos(3, 4, -1),
				BlockPos(3, 4, 0), BlockPos(3, 4, 1)
			),
			listOf(BlockPos(0, 1, 0), BlockPos(0, 2, 0), BlockPos(0, 3, 0), BlockPos(0, 4, 0))
		),
		TreeShape(
			listOf(
				BlockPos(-2, 4, -1), BlockPos(-2, 5, -1), BlockPos(-2, 4, 0), BlockPos(-2, 5, 0), BlockPos(-2, 4, 1),
				BlockPos(-2, 5, 1), BlockPos(-1, 4, -2), BlockPos(-1, 5, -2), BlockPos(-1, 3, -1), BlockPos(-1, 4, -1),
				BlockPos(-1, 5, -1), BlockPos(-1, 6, -1), BlockPos(-1, 3, 0), BlockPos(-1, 4, 0), BlockPos(-1, 5, 0),
				BlockPos(-1, 6, 0), BlockPos(-1, 3, 1), BlockPos(-1, 4, 1), BlockPos(-1, 5, 1), BlockPos(-1, 6, 1),
				BlockPos(-1, 4, 2), BlockPos(-1, 5, 2), BlockPos(0, 4, -2), BlockPos(0, 5, -2), BlockPos(0, 3, -1),
				BlockPos(0, 4, -1), BlockPos(0, 5, -1), BlockPos(0, 6, -1), BlockPos(0, 5, 0), BlockPos(0, 6, 0),
				BlockPos(0, 3, 1), BlockPos(0, 4, 1), BlockPos(0, 5, 1), BlockPos(0, 6, 1), BlockPos(0, 4, 2),
				BlockPos(0, 5, 2), BlockPos(1, 4, -2), BlockPos(1, 5, -2), BlockPos(1, 3, -1), BlockPos(1, 4, -1),
				BlockPos(1, 5, -1), BlockPos(1, 6, -1), BlockPos(1, 3, 0), BlockPos(1, 4, 0), BlockPos(1, 5, 0),
				BlockPos(1, 6, 0), BlockPos(1, 3, 1), BlockPos(1, 4, 1), BlockPos(1, 5, 1), BlockPos(1, 6, 1),
				BlockPos(1, 4, 2), BlockPos(1, 5, 2), BlockPos(2, 4, -1), BlockPos(2, 5, -1), BlockPos(2, 4, 0),
				BlockPos(2, 5, 0), BlockPos(2, 4, 1), BlockPos(2, 5, 1)
			),
			listOf(BlockPos(0, 1, 0), BlockPos(0, 2, 0), BlockPos(0, 3, 0), BlockPos(0, 4, 0))
		)
	)

	val fertilizerLevels: Map<String, Int> = mapOf(
		"farmersdelight:organic_compost" to 5,
		"farmersdelight:rich_soil" to 4,
		"minecraft:podzol" to 3,
		"minecraft:dirt" to 2,
		"minecraft:grass_block" to 2
	)

	val fertilizerDowngrades: Map<String, String> = mapOf(
		"farmersdelight:organic_compost" to "farmersdelight:rich_soil",
		"farmersdelight:rich_soil" to "minecraft:podzol",
		"minecraft:podzol" to "minecraft:dirt",
		"minecraft:dirt" to "minecraft:coarse_dirt",
		"minecraft:grass_block" to "minecraft:coarse_dirt"
	)

	val treeDefinitions: Map<String, TreeDefinition>
		get() = buildTreeDefinitions()

	val whiteList: Set<String>
		get() = treeDefinitions.keys


	@Suppress("unused")
	fun isWhiteListedSeed(stack: ItemStack): Boolean {
		return whiteList.contains(BuiltInRegistries.ITEM.getKey(stack.item).toString())
	}

	@Suppress("unused")
	fun definitionOf(stack: ItemStack): TreeDefinition? {
		return treeDefinitions[BuiltInRegistries.ITEM.getKey(stack.item).toString()]
	}

	fun resolveBlock(id: String): Block {
		val key = ResourceLocation.tryParse(id) ?: return Blocks.AIR
		return BuiltInRegistries.BLOCK.getOptional(key).orElse(Blocks.AIR)
	}

	fun extraLeafPlacement(spec: String, count: Int): TreeExtraPlacement {
		return TreeExtraPlacement(parseBlockStateSpec(spec), count)
	}

	fun resolveExtraLeafState(definition: TreeDefinition): BlockState? {
		return definition.extraLeafPlacement?.let { resolveBlockState(it.state) }
	}

	fun isExtraLeafState(state: BlockState, definition: TreeDefinition): Boolean {
		val extraLeafState = resolveExtraLeafState(definition) ?: return false
		return state == extraLeafState
	}

	fun isManorsBountyTree(definition: TreeDefinition): Boolean = definition.leafId.startsWith("manors_bounty:")

	fun createTree(level: Level, stumpPos: BlockPos, seedId: String, dy: Int): Boolean {
		val definition = treeDefinitions[seedId] ?: return false
		if (FruitsDelightTreeManager.supports(seedId)) {
			return FruitsDelightTreeManager.createTree(level, stumpPos, seedId, dy)
		}
		val shape = shapes.getOrNull(definition.shapeIndex) ?: return false
		val baseLeafState = definition.baseLeafState()
		val extraLeafState = definition.extraLeafPlacement?.let { resolveBlockState(it.state) }
		val leafBlock = resolveBlock(definition.leafId)

		for (relative in shape.leaveRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
			if (level.getBlockState(targetPos).isAir) {
				val leafState = if (isManorsBountyTree(definition)) {
					createManorsBountyLeafState(leafBlock.defaultBlockState())
				} else {
					val genericLeafState = withBlockstateProperty(leafBlock.defaultBlockState(), baseLeafState)
					withBooleanProperty(genericLeafState, "persistent", true)
				}
				level.setBlockAndUpdate(targetPos, leafState)
			}
		}

		if (extraLeafState != null) {
			val extraPlacement = definition.extraLeafPlacement
			val extraCount = minOf(extraPlacement.count, shape.leaveRelativePos.size)
			if (extraCount > 0) {
				val positions = shape.leaveRelativePos.toMutableList()
				repeat(extraCount) {
					if (positions.isEmpty()) {
						return@repeat
					}
					val selectedIndex = level.random.nextInt(positions.size)
					val relative = positions.removeAt(selectedIndex)
					val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
					if (level.getBlockState(targetPos).block == leafBlock) {
						level.setBlockAndUpdate(targetPos, extraLeafState)
					}
				}
			}
		}

		for (relative in shape.logRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y, relative.z)
			if (level.getBlockState(targetPos).isAir) {
				level.setBlockAndUpdate(targetPos, resolveBlock(definition.logId).defaultBlockState())
			}
		}

		level.setBlockAndUpdate(stumpPos, resolveBlock("farmerstales:tree_stump").defaultBlockState())
		(level.getBlockEntity(stumpPos) as? TreeStumpBlockEntity)?.initializeTree(seedId, definition.shapeIndex, dy)
		level.setBlockAndUpdate(stumpPos.below(), resolveBlock(ROOTED_DIRT).defaultBlockState())
		if (level is ServerLevel) {
			for (relative in shape.leaveRelativePos) {
				val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
				level.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3)
			}
			for (relative in shape.logRelativePos) {
				val targetPos = stumpPos.offset(relative.x, relative.y, relative.z)
				level.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3)
			}
			level.sendBlockUpdated(stumpPos, level.getBlockState(stumpPos), level.getBlockState(stumpPos), 3)
			level.sendBlockUpdated(stumpPos.below(), level.getBlockState(stumpPos.below()), level.getBlockState(stumpPos.below()), 3)
		}
		return true
	}

	fun placeTreeStump(level: Level, stumpPos: BlockPos): Boolean {
		if (!level.getBlockState(stumpPos).isAir) {
			return false
		}

		level.setBlockAndUpdate(stumpPos, resolveBlock("farmerstales:tree_stump").defaultBlockState())
		level.setBlockAndUpdate(stumpPos.below(), resolveBlock(ROOTED_DIRT).defaultBlockState())
		return true
	}

	fun clearTree(level: Level, stumpPos: BlockPos, seedId: String, treeShapeIndex: Int, dy: Int) {
		val definition = treeDefinitions[seedId] ?: return
		val shape = shapes.getOrNull(treeShapeIndex) ?: shapes.getOrNull(definition.shapeIndex) ?: return
		val leafBlock = resolveBlock(definition.leafId)
		val logBlock = resolveBlock(definition.logId)

		for (relative in shape.leaveRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
			if (level.getBlockState(targetPos).block == leafBlock) {
				level.destroyBlock(targetPos, false)
			}
		}

		for (relative in shape.logRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y, relative.z)
			if (level.getBlockState(targetPos).block == logBlock) {
				level.destroyBlock(targetPos, false)
			}
		}
	}

	fun setLeafState(state: BlockState, value: Int): BlockState {
		val property = state.properties.firstOrNull { it is IntegerProperty && it.name == "blockstate" } as? IntegerProperty ?: return state
		return try {
			state.setValue(property, value)
		} catch (_: IllegalArgumentException) {
			state
		}
	}

	@Suppress("unused")
	fun promoteLeafState(state: BlockState, definition: TreeDefinition): BlockState {
		val current = state.values.entries.firstOrNull { it.key.name == "blockstate" }?.value as? Int ?: return state
		val baseLeafState = definition.baseLeafState()
		return when {
			current == baseLeafState -> setLeafState(state, definition.growingState)
			current < definition.ripeState -> setLeafState(state, definition.ripeState)
			else -> state
		}
	}

	private fun parseBlockStateSpec(rawSpec: String): TreeBlockStateSpec {
		val trimmed = rawSpec.trim()
		val firstColon = trimmed.indexOf(':')
		val splitIndex = if (firstColon >= 0) trimmed.indexOf(':', firstColon + 1) else -1
		val blockId = if (splitIndex >= 0) trimmed.substring(0, splitIndex) else trimmed
		val propertyPart = if (splitIndex >= 0) trimmed.substring(splitIndex + 1) else ""
		val properties = if (propertyPart.isBlank()) {
			emptyMap()
		} else {
			propertyPart.split(',').mapNotNull { entry ->
				val pair = entry.trim()
				if (pair.isBlank()) {
					return@mapNotNull null
				}
				val equalsIndex = pair.indexOf('=')
				if (equalsIndex <= 0 || equalsIndex >= pair.lastIndex) {
					return@mapNotNull null
				}
				pair.substring(0, equalsIndex).trim() to pair.substring(equalsIndex + 1).trim()
			}.toMap()
		}
		return TreeBlockStateSpec(blockId, properties)
	}

	private fun resolveBlockState(spec: TreeBlockStateSpec): BlockState? {
		val block = resolveBlock(spec.blockId)
		if (block == Blocks.AIR && spec.blockId != "minecraft:air") {
			return null
		}

		var state = block.defaultBlockState()
		for ((propertyName, rawValue) in spec.properties) {
			state = applyBlockStateProperty(state, propertyName, rawValue)
		}
		return state
	}

	@Suppress("UNCHECKED_CAST")
	private fun applyBlockStateProperty(state: BlockState, propertyName: String, rawValue: String): BlockState {
		val property = state.properties.firstOrNull { it.name == propertyName } ?: return state
		val matchedValue = property.possibleValues.firstOrNull { it.toString().equals(rawValue, ignoreCase = true) } ?: return state
		return try {
			state.setValue(property as Property<Comparable<Any>>, matchedValue as Comparable<Any>)
		} catch (_: IllegalArgumentException) {
			state
		}
	}

	fun isWaterLike(state: BlockState): Boolean {
		return state.fluidState.`is`(FluidTags.WATER)
	}

	fun scanBelowForFertilizer(level: Level, stumpPos: BlockPos): Pair<BlockPos, String>? {
		var bestPos: BlockPos? = null
		var bestId: String? = null
		var bestLevel = -1
		for (dx in -1..1) {
			for (dz in -1..1) {
				val targetPos = stumpPos.offset(dx, -1, dz)
				val targetId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).block).toString()
				val lvl = fertilizerLevels[targetId] ?: continue
				if (lvl > bestLevel) {
					bestLevel = lvl
					bestPos = targetPos
					bestId = targetId
				}
			}
		}
		return if (bestPos != null && bestId != null) bestPos to bestId else null
	}

	fun scanBelowForWater(level: Level, stumpPos: BlockPos): Boolean {
		for (dx in -1..1) {
			for (dz in -1..1) {
				val targetState = level.getBlockState(stumpPos.offset(dx, -1, dz))
				if (isWaterLike(targetState)) {
					return true
				}
			}
		}
		return false
	}

	private fun withBlockstateProperty(state: BlockState, value: Int): BlockState {
		val property = state.properties.firstOrNull { it is IntegerProperty && it.name == "blockstate" } as? IntegerProperty ?: return state
		return try {
			state.setValue(property, value)
		} catch (_: IllegalArgumentException) {
			state
		}
	}

	private fun createManorsBountyLeafState(state: BlockState): BlockState {
		var prepared = withPropertyValue(state, "age", "0")
		prepared = withPropertyValue(prepared, "can_fruit", "TRUE")
		prepared = withPropertyValue(prepared, "rare", "false")
		return prepareManorsBountyLeafState(prepared)
	}

	fun prepareManorsBountyLeafState(state: BlockState): BlockState {
		var prepared = state
		prepared = withPropertyValue(prepared, "can_fruit", "TRUE")
		prepared = withPropertyValue(prepared, "persistent", "false")
		return withPropertyValue(prepared, "distance", "1")
	}

	fun isManorsBountyGrowthCandidate(state: BlockState, leafBlock: Block): Boolean {
		if (state.block != leafBlock) {
			return false
		}
		val age = integerPropertyValue(state, "age") ?: return false
		return age < 2 && propertyValue(state, "can_fruit").equals("TRUE", ignoreCase = true)
	}

	fun growManorsBountyLeaf(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean {
		val growable = state.block as? BonemealableBlock ?: return false
		if (!growable.isValidBonemealTarget(level, pos, state, false)) {
			return false
		}
		growable.performBonemeal(level, level.random, pos, state)
		return true
	}

	private fun withBooleanProperty(state: BlockState, propertyName: String, value: Boolean): BlockState {
		val property = state.properties.firstOrNull { it is BooleanProperty && it.name == propertyName } as? BooleanProperty ?: return state
		return try {
			state.setValue(property, value)
		} catch (_: IllegalArgumentException) {
			state
		}
	}

	private fun integerPropertyValue(state: BlockState, propertyName: String): Int? {
		val property = state.properties.firstOrNull { it is IntegerProperty && it.name == propertyName } as? IntegerProperty ?: return null
		return state.getValue(property)
	}

	@Suppress("UNCHECKED_CAST")
	private fun propertyValue(state: BlockState, propertyName: String): String? {
		val property = state.properties.firstOrNull { it.name == propertyName } ?: return null
		val typedProperty = property as Property<Comparable<Any>>
		return state.getValue(typedProperty).toString()
	}

	@Suppress("UNCHECKED_CAST")
	private fun withPropertyValue(state: BlockState, propertyName: String, rawValue: String): BlockState {
		val property = state.properties.firstOrNull { it.name == propertyName } ?: return state
		val matchedValue = property.possibleValues.firstOrNull { it.toString().equals(rawValue, ignoreCase = true) } ?: return state
		return try {
			state.setValue(property as Property<Comparable<Any>>, matchedValue as Comparable<Any>)
		} catch (_: IllegalArgumentException) {
			state
		}
	}

	private fun buildTreeDefinitions(): Map<String, TreeDefinition> {
		val genericDefinitions = mapOf(
			"manors_bounty:pear_seed" to TreeDefinition("manors_bounty:pear_leaves", "manors_bounty:rosaceae_tree_log", 1, 1, 3, 5),
			"manors_bounty:lemon_seed" to TreeDefinition("manors_bounty:lemon_leaves", "manors_bounty:rutaceae_tree_log", 3, 1, 3, 5),
			"manors_bounty:avocado_seed" to TreeDefinition("manors_bounty:avocado_tree_leaves", "manors_bounty:avocado_tree_log", 3, 1, 3, 5),
			"manors_bounty:peach_seed" to TreeDefinition("manors_bounty:peach_leaves", "manors_bounty:rosaceae_tree_log", 2, 1, 3, 5),
			"manors_bounty:apple_seed" to TreeDefinition("manors_bounty:apple_leaves", "manors_bounty:rosaceae_tree_log", 1, 1, 3, 5),
			"manors_bounty:orange_seed" to TreeDefinition("manors_bounty:orange_leaves", "manors_bounty:rutaceae_tree_log", 1, 1, 3, 5),
			"manors_bounty:olive_fruit_seed" to TreeDefinition("manors_bounty:olive_tree_leaves", "manors_bounty:olive_tree_log", 2, 1, 3, 5),
			"manors_bounty:mango_seed" to TreeDefinition("manors_bounty:mango_tree_leaves", "manors_bounty:mango_tree_log", 3, 1, 3, 5),
			"manors_bounty:starfruit_seed" to TreeDefinition("manors_bounty:starfruit_tree_leaves", "manors_bounty:starfruit_tree_log", 2, 1, 3, 5),
			"manors_bounty:cherries_seed" to TreeDefinition("manors_bounty:cherries_tree_leaves", "manors_bounty:cherries_tree_log", 1, 1, 3, 5),
			"manors_bounty:kiwifruit_seed" to TreeDefinition("manors_bounty:kiwifruit_tree_leaves", "manors_bounty:kiwifruit_tree_log", 2, 1, 3, 5),
			"manors_bounty:hawthorn_seed" to TreeDefinition("manors_bounty:hawthorn_tree_leaves", "manors_bounty:hawthorn_tree_log", 2, 1, 3, 5),
			"manors_bounty:pomegranate_sapling" to TreeDefinition("manors_bounty:pomegranate_tree_leaves", "manors_bounty:pomegranate_tree_log", 1, 1, 3, 5),
			//"" to TreeDefinition("", "", 1, 1, 3, 5),
		).filterKeys { seedId ->
			// Only include entries where the seed item actually exists
			val key = ResourceLocation.tryParse(seedId)
			key != null && BuiltInRegistries.ITEM.containsKey(key)
		}
		return genericDefinitions + FruitsDelightTreeManager.treeDefinitions()
	}

}


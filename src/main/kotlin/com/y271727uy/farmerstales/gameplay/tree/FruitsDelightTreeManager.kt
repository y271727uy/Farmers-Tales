package com.y271727uy.farmerstales.gameplay.tree

import com.y271727uy.farmerstales.gameplay.tree.block.entity.TreeStumpBlockEntity
import com.y271727uy.farmerstales.gameplay.tree.event.EnvironmentVariables.SeasonVariables
import com.y271727uy.farmerstales.gameplay.tropical_climate.TropicalClimateEffects
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import net.minecraftforge.registries.ForgeRegistries

@Suppress("unused")
object FruitsDelightTreeManager {
	private const val FLOWERING_PAUSE_THRESHOLD = 50
	private const val FLOWERING_BOOST_THRESHOLD = 80
	private const val FLOWERING_BOOST_MULTIPLIER = 2
	private const val FLOWERING_PROGRESS_SCALE = 0.5f
	private const val RESOURCE_CONSUMPTION_SCALE = 0.5f
	private const val GROWTH_ROLL_MAX = 125
	private const val BRANCHES_MAX = 25

	private val FRUITS_DELIGHT_TREE_DEFINITIONS: Map<String, TreeDefinition> = mapOf(
		"fruitsdelight:apple_sapling" to standardDefinition("apple", "minecraft:oak_log", 1),
		"fruitsdelight:bayberry_sapling" to standardDefinition("bayberry", "minecraft:spruce_log", 3),
		"fruitsdelight:fig_sapling" to standardDefinition("fig", "minecraft:oak_log", 1),
		"fruitsdelight:pear_sapling" to TreeDefinition(
			"fruitsdelight:pear_leaves",
			"minecraft:birch_log",
			3,
			1,
			3,
			5,
			exactLeafState = parseBlockStateSpec("fruitsdelight:pear_leaves:waterlogged=false,distance=7,persistent=true,type=LEAVES"),
			floweringLeafState = parseBlockStateSpec("fruitsdelight:pear_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"),
			fruitLeafState = parseBlockStateSpec("fruitsdelight:pear_leaves:waterlogged=false,distance=7,persistent=true,type=FRUITS"),
			extraLeafPlacement = TreeExtraPlacement(parseBlockStateSpec("fruitsdelight:pear_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"), 4)
		),
		"fruitsdelight:hawberry_sapling" to TreeDefinition(
			"fruitsdelight:hawberry_leaves",
			"minecraft:spruce_log",
			3,
			1,
			3,
			5,
			exactLeafState = parseBlockStateSpec("fruitsdelight:hawberry_leaves:waterlogged=false,distance=7,persistent=true,type=LEAVES"),
			floweringLeafState = parseBlockStateSpec("fruitsdelight:hawberry_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"),
			fruitLeafState = parseBlockStateSpec("fruitsdelight:hawberry_leaves:waterlogged=false,distance=7,persistent=true,type=FRUITS"),
			extraLeafPlacement = TreeExtraPlacement(parseBlockStateSpec("fruitsdelight:hawberry_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"), 4)
		),
		"fruitsdelight:lychee_sapling" to TreeDefinition(
			"fruitsdelight:lychee_leaves",
			"minecraft:jungle_log",
			3,
			1,
			3,
			5,
			exactLeafState = parseBlockStateSpec("fruitsdelight:lychee_leaves:waterlogged=false,distance=7,persistent=true,type=LEAVES"),
			floweringLeafState = parseBlockStateSpec("fruitsdelight:lychee_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"),
			fruitLeafState = parseBlockStateSpec("fruitsdelight:lychee_leaves:waterlogged=false,distance=7,persistent=true,type=FRUITS"),
			extraLeafPlacement = TreeExtraPlacement(parseBlockStateSpec("fruitsdelight:lychee_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"), 4)
		),
		"fruitsdelight:kiwi_sapling" to standardDefinition("kiwi", "minecraft:jungle_log", 1),
		"fruitsdelight:mango_sapling" to standardDefinition("mango", "minecraft:jungle_log", 1),
		"fruitsdelight:mangosteen_sapling" to standardDefinition("mangosteen", "minecraft:oak_log", 4),
		"fruitsdelight:orange_sapling" to standardDefinition("orange", "minecraft:oak_log", 1),
		"fruitsdelight:peach_sapling" to standardDefinition("peach", "minecraft:jungle_log", 1),
		"fruitsdelight:persimmon_sapling" to standardDefinition("persimmon", "minecraft:spruce_log", 4)
	).filterKeys { seedId ->
		val key = ResourceLocation.tryParse(seedId)
		key != null && ForgeRegistries.ITEMS.containsKey(key)
	}

	fun supports(treeType: String?): Boolean {
		return !treeType.isNullOrBlank() && FRUITS_DELIGHT_TREE_DEFINITIONS.containsKey(treeType)
	}

	fun treeDefinitions(): Map<String, TreeDefinition> {
		return FRUITS_DELIGHT_TREE_DEFINITIONS
	}

	private fun standardDefinition(name: String, logId: String, shapeIndex: Int): TreeDefinition {
		return TreeDefinition(
			leafId = "fruitsdelight:${name}_leaves",
			logId = logId,
			shapeIndex = shapeIndex,
			growingState = 1,
			defaultState = 3,
			ripeState = 5,
			exactLeafState = parseBlockStateSpec(
				"fruitsdelight:${name}_leaves:waterlogged=false,distance=7,persistent=true,type=LEAVES"
			),
			floweringLeafState = parseBlockStateSpec(
				"fruitsdelight:${name}_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"
			),
			fruitLeafState = parseBlockStateSpec(
				"fruitsdelight:${name}_leaves:waterlogged=false,distance=7,persistent=true,type=FRUITS"
			),
			extraLeafPlacement = TreeExtraPlacement(
				parseBlockStateSpec(
					"fruitsdelight:${name}_leaves:waterlogged=false,distance=7,persistent=true,type=FLOWERS"
				),
				4
			)
		)
	}

	fun baseLeafState(definition: TreeDefinition): BlockState? {
		return definition.exactLeafState?.let { resolveBlockState(it) }
	}

	fun floweringLeafState(definition: TreeDefinition): BlockState? {
		return definition.floweringLeafState?.let { resolveBlockState(it) }
	}

	fun fruitLeafState(definition: TreeDefinition): BlockState? {
		return definition.fruitLeafState?.let { resolveBlockState(it) }
	}

	@Suppress("unused")
	fun createTree(level: Level, stumpPos: BlockPos, seedId: String, dy: Int): Boolean {
		val definition = FRUITS_DELIGHT_TREE_DEFINITIONS[seedId] ?: return false
		val shape = TreeDefinitions.shapes.getOrNull(definition.shapeIndex) ?: return false
		val leafBlock = TreeDefinitions.resolveBlock(definition.leafId)
		val baseState = baseLeafState(definition) ?: leafBlock.defaultBlockState()
		val floweringState = floweringLeafState(definition) ?: return false
		val leafCount = definition.extraLeafPlacement?.count?.coerceAtMost(shape.leaveRelativePos.size) ?: 0
		val positions = shape.leaveRelativePos.toMutableList()

		for (relative in shape.leaveRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
			if (level.getBlockState(targetPos).isAir) {
				level.setBlockAndUpdate(targetPos, baseState)
			}
		}

		repeat(leafCount) {
			if (positions.isEmpty()) {
				return@repeat
			}
			val selectedIndex = level.random.nextInt(positions.size)
			val relative = positions.removeAt(selectedIndex)
			val targetPos = stumpPos.offset(relative.x, relative.y + dy, relative.z)
			if (level.getBlockState(targetPos).block == leafBlock) {
				level.setBlockAndUpdate(targetPos, floweringState)
			}
		}

		for (relative in shape.logRelativePos) {
			val targetPos = stumpPos.offset(relative.x, relative.y, relative.z)
			if (level.getBlockState(targetPos).isAir) {
				level.setBlockAndUpdate(targetPos, TreeDefinitions.resolveBlock(definition.logId).defaultBlockState())
			}
		}

		level.setBlockAndUpdate(stumpPos, TreeDefinitions.resolveBlock("farmerstales:tree_stump").defaultBlockState())
		(level.getBlockEntity(stumpPos) as? TreeStumpBlockEntity)?.initializeTree(seedId, definition.shapeIndex, dy)
		level.setBlockAndUpdate(stumpPos.below(), TreeDefinitions.resolveBlock("minecraft:rooted_dirt").defaultBlockState())
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

	fun tickTree(level: ServerLevel, stumpPos: BlockPos, entity: TreeStumpBlockEntity, maintenanceScore: Int, maintenanceBonus: Int, growthAttempts: Int): Boolean {
		if (!SeasonVariables.isTreeGrowthAllowed(level, stumpPos)) {
			return false
		}

		if (Math.floorMod(level.gameTime + stumpPos.asLong(), TREE_TICK_INTERVAL_TICKS.toLong()) != 0L) {
			return false
		}
		if (growthAttempts <= 0) {
			return false
		}

		val definition = FRUITS_DELIGHT_TREE_DEFINITIONS[entity.treeType] ?: return false
		val treeShape = TreeDefinitions.shapes.getOrNull(entity.treeShape) ?: return false
		val leafBlock = TreeDefinitions.resolveBlock(definition.leafId)
		val baseState = baseLeafState(definition) ?: leafBlock.defaultBlockState()
		val floweringState = floweringLeafState(definition) ?: return false
		val fruitState = fruitLeafState(definition) ?: return false
		val leafPositions = treeShape.leaveRelativePos.map { relative -> stumpPos.offset(relative.x, relative.y + entity.dy, relative.z) }

		var changed = false

		repeat(growthAttempts) {
			val floweringPositions = mutableListOf<BlockPos>()
			val basePositions = mutableListOf<BlockPos>()

			for (targetPos in leafPositions) {
				val targetState = level.getBlockState(targetPos)
				when {
					targetState == floweringState -> floweringPositions.add(targetPos)
					targetState == fruitState -> Unit
					targetState.block == leafBlock -> basePositions.add(targetPos)
				}
			}

			val desiredFloweringCount = desiredFloweringCount(definition, leafPositions.size, maintenanceScore)

			if (floweringPositions.size < desiredFloweringCount && basePositions.isNotEmpty()) {
				if (level.random.nextFloat() < FLOWERING_PROGRESS_SCALE) {
					val targetPos = basePositions[level.random.nextInt(basePositions.size)]
					if (level.getBlockState(targetPos).block == leafBlock) {
						level.setBlockAndUpdate(targetPos, floweringState)
						changed = true
					}
				}
			} else if (floweringPositions.size > desiredFloweringCount && floweringPositions.isNotEmpty()) {
				val targetPos = floweringPositions[level.random.nextInt(floweringPositions.size)]
				if (level.getBlockState(targetPos) == floweringState) {
					level.setBlockAndUpdate(targetPos, baseState)
					changed = true
				}
			} else if (floweringPositions.isNotEmpty() && maintenanceScore >= FLOWERING_PAUSE_THRESHOLD) {
				val chance = if (maintenanceScore < FLOWERING_BOOST_THRESHOLD) {
					maintenanceScore
				} else {
					(maintenanceScore + maintenanceBonus).coerceAtMost(GROWTH_ROLL_MAX)
				}
				val scaledChance = (chance * FLOWERING_PROGRESS_SCALE).toInt().coerceAtLeast(1)
				if (level.random.nextInt(GROWTH_ROLL_MAX) < scaledChance) {
					val targetPos = floweringPositions[level.random.nextInt(floweringPositions.size)]
					if (level.getBlockState(targetPos) == floweringState) {
						level.setBlockAndUpdate(targetPos, fruitState)
						changed = true
					}
				}
			}
		}

		if (changed) {
			if (level.random.nextFloat() < TropicalClimateEffects.treeFertilizerConsumptionChance(level, stumpPos, 0.4f * RESOURCE_CONSUMPTION_SCALE)) {
				entity.fertilize = (entity.fertilize - 1).coerceAtLeast(0)
			}
			if (level.random.nextFloat() < TropicalClimateEffects.treeWaterConsumptionChance(level, stumpPos, 0.4f * RESOURCE_CONSUMPTION_SCALE)) {
				entity.water = (entity.water - 1).coerceAtLeast(0)
			}
			if (level.random.nextFloat() < 0.125f * RESOURCE_CONSUMPTION_SCALE) {
				entity.branches = (entity.branches + 1).coerceAtMost(BRANCHES_MAX)
			}
			entity.syncToClient()
		}
		return changed
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

	private fun desiredFloweringCount(definition: TreeDefinition, leafCount: Int, maintenanceScore: Int): Int {
		val baseCount = definition.extraLeafPlacement?.count ?: 0
		val target = when {
			maintenanceScore < FLOWERING_PAUSE_THRESHOLD -> 0
			maintenanceScore < FLOWERING_BOOST_THRESHOLD -> baseCount
			else -> baseCount * FLOWERING_BOOST_MULTIPLIER
		}
		return target.coerceIn(0, leafCount)
	}

	fun resolveBlock(id: String): net.minecraft.world.level.block.Block {
		val key = ResourceLocation.tryParse(id) ?: return Blocks.AIR
		return TreeDefinitions.resolveBlock(key.toString())
	}
}

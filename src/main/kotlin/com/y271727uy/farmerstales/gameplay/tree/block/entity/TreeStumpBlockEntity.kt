package com.y271727uy.farmerstales.gameplay.tree.block.entity

import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.gameplay.tree.FruitsDelightTreeManager
import com.y271727uy.farmerstales.gameplay.tree.TREE_TICK_INTERVAL_TICKS
import com.y271727uy.farmerstales.gameplay.tropical_climate.TropicalClimateEffects
import com.y271727uy.farmerstales.gameplay.special_event.common.SpecialEventGrowth
import com.y271727uy.farmerstales.gameplay.tree.TreeDefinitions
import com.y271727uy.farmerstales.gameplay.tree.event.EnvironmentVariables.SeasonVariables
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.Connection
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty

class TreeStumpBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : BlockEntity(type, pos, state) {
	var treeType: String = ""
	var treeShape: Int = 0
	var dy: Int = 0
	var fertilize: Int = 0
	var water: Int = 0
	var branches: Int = 0
	var pendingGrowthTicks: Int = -1

	companion object {
		private const val INITIAL_GROWTH_DELAY_MIN_TICKS = 20 * 10
		private const val INITIAL_GROWTH_DELAY_MAX_TICKS = 20 * 30
		private const val FERTILIZE_MAX = 50
		private const val WATER_MAX = 25
		private const val FERTILIZER_GAIN = 10
		private const val WATER_GAIN = 5
		private const val BRANCHES_MAX = 25
		private const val BRANCHES_GAIN_CHANCE = 0.125f
		private const val FRESH_TREE_FERTILIZE = 15
		private const val FRESH_TREE_WATER = 15
		private const val GROWTH_ROLL_MAX = 125
		private const val MAINTENANCE_LOW_THRESHOLD = 50
		private const val MAINTENANCE_MAX = 100
		private const val MAINTENANCE_BONUS_THRESHOLD = 80
		private const val MAINTENANCE_BONUS = 25
		private const val GROWTH_ATTEMPTS_BASE = 2
		private const val GROWTH_ATTEMPTS_WELL_MAINTAINED = 4
		private const val GROWTH_ATTEMPTS_PERFECT = 6

		@JvmStatic
		fun serverTick(level: Level, pos: BlockPos, state: BlockState, entity: TreeStumpBlockEntity) {
			if (level.isClientSide || level !is ServerLevel) {
				return
			}

			if (state.block != ModBlocks.TREE_STUMP.get()) {
				return
			}

			if (!entity.validateAndNormalize()) {
				return
			}

			if (!SeasonVariables.isTreeGrowthAllowed(level, pos)) {
				return
			}

			val treeDefinition = TreeDefinitions.treeDefinitions[entity.treeType] ?: return
			val treeShape = TreeDefinitions.shapes.getOrNull(entity.treeShape) ?: return
			val extraLeafState = TreeDefinitions.resolveExtraLeafState(treeDefinition)
			val floweringLeafState = if (FruitsDelightTreeManager.supports(entity.treeType)) FruitsDelightTreeManager.floweringLeafState(treeDefinition) else null
			val fruitLeafState = if (FruitsDelightTreeManager.supports(entity.treeType)) FruitsDelightTreeManager.fruitLeafState(treeDefinition) else null
			val leafBlock = TreeDefinitions.resolveBlock(treeDefinition.leafId)
			val baseLeafState = treeDefinition.baseLeafState()

			if (entity.pendingGrowthTicks >= 0) {
				if (entity.pendingGrowthTicks > 0) {
					entity.pendingGrowthTicks -= 1
					if (entity.pendingGrowthTicks > 0) {
						return
					}
				}

				if (TreeDefinitions.createTree(level, pos, entity.treeType, entity.dy)) {
					entity.pendingGrowthTicks = -1
					entity.syncToClient()
				}
				return
			}

			if (Math.floorMod(level.gameTime + pos.asLong(), TREE_TICK_INTERVAL_TICKS.toLong()) != 0L) {
				return
			}

			var fertilize = entity.fertilize
			var water = entity.water
			var branches = entity.branches

			TreeDefinitions.scanBelowForFertilizer(level, pos)?.let { (fertilizerPos, fertilizerId) ->
				if (fertilize < FERTILIZE_MAX) {
					fertilize = (fertilize + FERTILIZER_GAIN).coerceAtMost(FERTILIZE_MAX)
					TreeDefinitions.fertilizerDowngrades[fertilizerId]?.let { downgradeId ->
						level.setBlockAndUpdate(fertilizerPos, TreeDefinitions.resolveBlock(downgradeId).defaultBlockState())
					}
				}
			}

			if (fertilize >= 1 && water < WATER_MAX && TreeDefinitions.scanBelowForWater(level, pos)) {
				fertilize = (fertilize - 1).coerceAtLeast(0)
				water = (water + WATER_GAIN).coerceAtMost(WATER_MAX)
			}

			entity.fertilize = fertilize
			entity.water = water
			entity.branches = branches

			val maintenanceScore = entity.maintenanceScore()
			val maintenanceBonus = entity.maintenanceBonus()
            val baseGrowthAttempts = entity.growthAttemptCount()
            val growthAttempts = if (SpecialEventGrowth.isBonusGrowthActive(level, pos)) {
                baseGrowthAttempts * 2
            } else {
                baseGrowthAttempts
            }

			if (FruitsDelightTreeManager.supports(entity.treeType)) {
				FruitsDelightTreeManager.tickTree(level, pos, entity, maintenanceScore, maintenanceBonus, growthAttempts)
				entity.syncToClient()
				return
			}

			if (TreeDefinitions.isManorsBountyTree(treeDefinition)) {
				repeat(growthAttempts) {
					val eligibleLeaves = treeShape.leaveRelativePos.mapNotNull { relative ->
						val targetPos = pos.offset(relative.x, relative.y + entity.dy, relative.z)
						val currentState = level.getBlockState(targetPos)
						val preparedState = TreeDefinitions.prepareManorsBountyLeafState(currentState)
						if (preparedState != currentState) {
							level.setBlockAndUpdate(targetPos, preparedState)
						}
						if (TreeDefinitions.isManorsBountyGrowthCandidate(preparedState, leafBlock)) targetPos else null
					}
					if (eligibleLeaves.isEmpty()) {
						return@repeat
					}

					val targetPos = eligibleLeaves[level.random.nextInt(eligibleLeaves.size)]
					val targetState = level.getBlockState(targetPos)
					val chance = if (maintenanceScore < MAINTENANCE_LOW_THRESHOLD) 0 else (maintenanceScore + maintenanceBonus).coerceAtMost(GROWTH_ROLL_MAX)
					if (level.random.nextInt(GROWTH_ROLL_MAX) < chance && TreeDefinitions.growManorsBountyLeaf(level, targetPos, targetState)) {
						if (level.random.nextFloat() < TropicalClimateEffects.treeFertilizerConsumptionChance(level, pos, 0.4f)) {
							fertilize = (fertilize - 1).coerceAtLeast(0)
						}
						if (level.random.nextFloat() < TropicalClimateEffects.treeWaterConsumptionChance(level, pos, 0.4f)) {
							water = (water - 1).coerceAtLeast(0)
						}
						if (level.random.nextFloat() < BRANCHES_GAIN_CHANCE) {
							branches = (branches + 1).coerceAtMost(BRANCHES_MAX)
						}
					}
				}
				entity.fertilize = fertilize
				entity.water = water
				entity.branches = branches
				entity.syncToClient()
				return
			}

			if (growthAttempts > 0 && treeShape.leaveRelativePos.isNotEmpty()) {
				repeat(growthAttempts) {
					val eligibleLeaves = treeShape.leaveRelativePos.mapNotNull { relative ->
						val targetPos = pos.offset(relative.x, relative.y + entity.dy, relative.z)
						val targetState = level.getBlockState(targetPos)
						if (isGenericGrowthCandidate(targetState, leafBlock, extraLeafState, treeDefinition)) targetPos else null
					}
					if (eligibleLeaves.isEmpty()) {
						return@repeat
					}

					val targetPos = eligibleLeaves[level.random.nextInt(eligibleLeaves.size)]
					val targetState = level.getBlockState(targetPos)
					if (targetState.block == leafBlock) {
						if (floweringLeafState != null && fruitLeafState != null) {
							if (targetState == floweringLeafState) {
								val chance = if (maintenanceScore < MAINTENANCE_LOW_THRESHOLD) {
									0
								} else {
									(maintenanceScore + maintenanceBonus).coerceAtMost(GROWTH_ROLL_MAX)
								}
								if (level.random.nextInt(GROWTH_ROLL_MAX) < chance) {
									level.setBlockAndUpdate(targetPos, fruitLeafState)
									if (level.random.nextFloat() < TropicalClimateEffects.treeFertilizerConsumptionChance(level, pos, 0.4f)) {
										fertilize = (fertilize - 1).coerceAtLeast(0)
									}
									if (level.random.nextFloat() < TropicalClimateEffects.treeWaterConsumptionChance(level, pos, 0.4f)) {
										water = (water - 1).coerceAtLeast(0)
									}
									if (level.random.nextFloat() < BRANCHES_GAIN_CHANCE) {
										branches = (branches + 1).coerceAtMost(BRANCHES_MAX)
									}
								}
							}
						} else if (!(extraLeafState != null && targetState == extraLeafState)) {
							val currentState = targetState.values.entries.firstOrNull { it.key.name == "blockstate" }?.value as? Int
							val chance = if (maintenanceScore < MAINTENANCE_LOW_THRESHOLD) {
								0
							} else {
								(maintenanceScore + maintenanceBonus).coerceAtMost(GROWTH_ROLL_MAX)
							}
							if (level.random.nextInt(GROWTH_ROLL_MAX) < chance) {
								val nextState = if (currentState == baseLeafState) {
									TreeDefinitions.setLeafState(targetState, treeDefinition.growingState)
								} else {
									TreeDefinitions.promoteLeafState(targetState, treeDefinition)
								}
								level.setBlockAndUpdate(targetPos, nextState)
								if (level.random.nextFloat() < TropicalClimateEffects.treeFertilizerConsumptionChance(level, pos, 0.4f)) {
									fertilize = (fertilize - 1).coerceAtLeast(0)
								}
								if (level.random.nextFloat() < TropicalClimateEffects.treeWaterConsumptionChance(level, pos, 0.4f)) {
									water = (water - 1).coerceAtLeast(0)
								}
								if (level.random.nextFloat() < BRANCHES_GAIN_CHANCE) {
									branches = (branches + 1).coerceAtMost(BRANCHES_MAX)
								}
							} else if (currentState != treeDefinition.ripeState) {
								level.setBlockAndUpdate(targetPos, TreeDefinitions.setLeafState(targetState, baseLeafState))
							}
						}
					}
				}
			}

			entity.fertilize = fertilize
			entity.water = water
			entity.branches = branches
			entity.syncToClient()
		}

		private fun isGenericGrowthCandidate(targetState: BlockState, leafBlock: net.minecraft.world.level.block.Block, extraLeafState: BlockState?, definition: com.y271727uy.farmerstales.gameplay.tree.TreeDefinition): Boolean {
			if (targetState.block != leafBlock) {
				return false
			}
			if (extraLeafState != null && targetState == extraLeafState) {
				return false
			}
			val currentState = targetState.values.entries.firstOrNull { it.key is IntegerProperty && it.key.name == "blockstate" }?.value as? Int
				?: return false
			return currentState < definition.ripeState
		}

		@JvmStatic
		@Suppress("unused")
		fun scheduleInitialGrowth(entity: TreeStumpBlockEntity, random: net.minecraft.util.RandomSource) {
			val range = INITIAL_GROWTH_DELAY_MAX_TICKS - INITIAL_GROWTH_DELAY_MIN_TICKS + 1
			entity.pendingGrowthTicks = random.nextInt(range) + INITIAL_GROWTH_DELAY_MIN_TICKS
			entity.syncToClient()
		}
	}

	fun validateAndNormalize(): Boolean {
		val definition = TreeDefinitions.treeDefinitions[treeType] ?: run {
			resetTreeData()
			return false
		}

		treeShape = definition.shapeIndex
		dy = dy.coerceIn(-1, 1)
		fertilize = fertilize.coerceIn(0, FERTILIZE_MAX)
		water = water.coerceIn(0, WATER_MAX)
		branches = branches.coerceIn(0, BRANCHES_MAX)
		return true
	}

	fun maintenanceScore(): Int {
		return (fertilize + water + (BRANCHES_MAX - branches)).coerceIn(0, MAINTENANCE_MAX)
	}

	@Suppress("unused")
	fun isWellMaintained(): Boolean {
		return maintenanceScore() >= MAINTENANCE_BONUS_THRESHOLD
	}

	@Suppress("unused")
	fun isGrowthPaused(): Boolean {
		return maintenanceScore() < MAINTENANCE_LOW_THRESHOLD
	}

	fun initializeTree(treeType: String, treeShape: Int, dy: Int) {
		this.treeType = treeType
		this.treeShape = treeShape
		this.dy = dy.coerceIn(-1, 1)
		fertilize = 0
		water = 0
		branches = 0
		pendingGrowthTicks = -1
		syncToClient()
	}

	fun primeFreshGrowth() {
		fertilize = FRESH_TREE_FERTILIZE.coerceAtMost(FERTILIZE_MAX)
		water = FRESH_TREE_WATER.coerceAtMost(WATER_MAX)
		syncToClient()
	}

	fun maintenanceBonus(): Int {
		return growthSpeedBonus()
	}

	fun growthSpeedBonus(): Int {
		val score = maintenanceScore()
		return if (score >= MAINTENANCE_BONUS_THRESHOLD) MAINTENANCE_BONUS else 0
	}

	fun growthAttemptCount(): Int {
		val score = maintenanceScore()
		return when {
			score < MAINTENANCE_LOW_THRESHOLD -> 0
			score >= MAINTENANCE_MAX -> GROWTH_ATTEMPTS_PERFECT
			score >= MAINTENANCE_BONUS_THRESHOLD -> GROWTH_ATTEMPTS_WELL_MAINTAINED
			else -> GROWTH_ATTEMPTS_BASE
		}
	}

	fun applyPotionWatering(player: Player, stack: ItemStack): Boolean {
		if (stack.item != Items.POTION || PotionUtils.getPotion(stack) != Potions.WATER) {
			return false
		}
		if (!validateAndNormalize()) {
			return false
		}
		if (water >= WATER_MAX) {
			return false
		}

		water = (water + WATER_GAIN).coerceAtMost(WATER_MAX)
		if (player.level().random.nextFloat() < 0.5f) {
			fertilize = (fertilize - 1).coerceAtLeast(0)
		}

		if (!player.abilities.instabuild) {
			stack.shrink(1)
			val bottleStack = ItemStack(Items.GLASS_BOTTLE)
			if (!player.inventory.add(bottleStack)) {
				player.spawnAtLocation(bottleStack)
			}
		}

		syncToClient()
		return true
	}

	@Suppress("unused")
	fun applyGrowthCatalyst(player: Player, stack: ItemStack): Boolean {
		if (stack.item != Items.BONE_MEAL) {
			return false
		}
		if (!validateAndNormalize()) {
			return false
		}
		if (!SeasonVariables.isTreeGrowthAllowed(level, worldPosition)) {
			return false
		}
		if (pendingGrowthTicks < 0) {
			return false
		}

		val serverLevel = level as? ServerLevel ?: return false
		val grew = TreeDefinitions.createTree(serverLevel, worldPosition, treeType, dy)
		if (grew) {
			if (!player.abilities.instabuild) {
				stack.shrink(1)
			}
			pendingGrowthTicks = -1
			syncToClient()
		}
		return grew
	}

	fun harvestBranches(player: Player): Boolean {
		if (!validateAndNormalize()) {
			return false
		}
		if (branches <= 0) {
			return false
		}

		val definition = TreeDefinitions.treeDefinitions[treeType] ?: return false
		val rewardCount = player.level().random.nextInt(4) + 3
		val rewardStack = ItemStack(TreeDefinitions.resolveBlock(definition.logId).asItem(), rewardCount)

		if (!player.inventory.add(rewardStack)) {
			player.spawnAtLocation(rewardStack)
		}

		branches = (branches - 3).coerceAtLeast(0)
		syncToClient()
		return true
	}

	fun clearTree() {
		val serverLevel = level as? ServerLevel ?: return
		if (!validateAndNormalize()) {
			return
		}
		TreeDefinitions.clearTree(serverLevel, worldPosition, treeType, treeShape, dy)
	}

	private fun resetTreeData() {
		treeType = ""
		treeShape = 0
		dy = 0
		fertilize = 0
		water = 0
		branches = 0
		pendingGrowthTicks = -1
	}

	fun syncToClient() {
		setChanged()
		(level as? ServerLevel)?.sendBlockUpdated(worldPosition, blockState, blockState, 3)
	}

	private fun readTreeData(tag: CompoundTag) {
		treeType = tag.getString("tree_type")
		treeShape = tag.getInt("tree_shape")
		dy = tag.getInt("dy")
		fertilize = tag.getInt("fertilize")
		water = tag.getInt("water")
		branches = tag.getInt("branches")
		pendingGrowthTicks = if (tag.contains("pending_growth_ticks")) tag.getInt("pending_growth_ticks") else -1
		validateAndNormalize()
	}

	private fun writeTreeData(tag: CompoundTag) {
		tag.putString("tree_type", treeType)
		tag.putInt("tree_shape", treeShape)
		tag.putInt("dy", dy)
		tag.putInt("fertilize", fertilize)
		tag.putInt("water", water)
		tag.putInt("branches", branches)
		tag.putInt("pending_growth_ticks", pendingGrowthTicks)
	}

	override fun saveAdditional(tag: CompoundTag) {
		super.saveAdditional(tag)
		writeTreeData(tag)
	}

	override fun load(tag: CompoundTag) {
		super.load(tag)
		readTreeData(tag)
	}

	override fun getUpdateTag(): CompoundTag {
		val tag = super.getUpdateTag()
		writeTreeData(tag)
		return tag
	}

	override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
		return ClientboundBlockEntityDataPacket.create(this)
	}

	override fun handleUpdateTag(tag: CompoundTag) {
		super.handleUpdateTag(tag)
		readTreeData(tag)
	}

	override fun onDataPacket(net: Connection, pkt: ClientboundBlockEntityDataPacket) {
		val tag = pkt.tag
		if (tag != null) {
			handleUpdateTag(tag)
		}
	}
}

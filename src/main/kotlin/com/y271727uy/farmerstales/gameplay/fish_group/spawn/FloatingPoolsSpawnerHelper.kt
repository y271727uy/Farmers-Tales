package com.y271727uy.farmerstales.gameplay.fish_group.spawn

import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.item.FishGroupRegistry
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolFactory
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BiomeTags
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB

object FloatingPoolsSpawnerHelper {
    private const val MAX_COUNT = 6
    private const val SPAWN_INTERVAL = 3_000
    private val spawnStateByDimension = mutableMapOf<ResourceKey<Level>, SpawnState>()

    private fun attemptSpawn(level: ServerLevel) {
        if (level.players().isEmpty()) return
        val random = level.random
        val player = level.players()[random.nextInt(level.players().size)]
        val dimension = level.dimension()
        var spawnState = spawnStateByDimension[dimension]
        if (spawnState?.lastValidSpawn == null || level.gameTime - spawnState.lastSpawnTime > 12_000) {
            spawnState = SpawnState(findValidSpawnPos(level, player.blockPosition()), level.gameTime)
            spawnStateByDimension[dimension] = spawnState
        }
        val lastValidSpawn = spawnState.lastValidSpawn ?: return
        val spawnX = lastValidSpawn.x
        val spawnY = lastValidSpawn.y
        val spawnZ = lastValidSpawn.z
        val area = AABB(
            spawnX - 128.0,
            spawnY - 128.0,
            spawnZ - 128.0,
            spawnX + 128.0,
            spawnY + 128.0,
            spawnZ + 128.0,
        )
        if (level.getEntitiesOfClass(AbstractFishPoolEntity::class.java, area).size >= MAX_COUNT) return

        val biome = level.getBiome(lastValidSpawn)
        val availablePools = when {
            biome.`is`(BiomeTags.IS_OCEAN) -> FishPoolFactory.getAvailable(
                level,
                lastValidSpawn,
                FishPoolDefinition.Environment.OCEAN,
            )

            biome.`is`(BiomeTags.IS_RIVER) -> FishPoolFactory.getAvailable(
                level,
                lastValidSpawn,
                FishPoolDefinition.Environment.RIVER,
            )

            else -> emptyList()
        }
        if (availablePools.isEmpty()) return
        spawnFishPool(level, random, availablePools, spawnX, spawnY, spawnZ)
    }

    private fun findValidSpawnPos(level: ServerLevel, center: BlockPos): BlockPos? {
        repeat(10) {
            val newPos = center.offset(level.random.nextInt(200) - 100, 0, level.random.nextInt(200) - 100)
            val biome = level.getBiome(newPos)
            if (biome.`is`(BiomeTags.IS_RIVER) || biome.`is`(BiomeTags.IS_OCEAN)) {
                val spawnY = level.getHeight(Heightmap.Types.WORLD_SURFACE, newPos.x, newPos.z) - 2
                return BlockPos(newPos.x, spawnY, newPos.z)
            }
        }
        return null
    }

    private fun spawnFishPool(
        level: ServerLevel,
        random: RandomSource,
        availablePools: List<FishPoolDefinition>,
        spawnX: Int,
        spawnY: Int,
        spawnZ: Int,
    ) {
        val definition = availablePools[random.nextInt(availablePools.size)]
        val fishPool = FishGroupRegistry.getFishPoolRegistration(definition.id).create(level) ?: return
        fishPool.setFishPoolDefinition(definition.id)
        fishPool.setPos(spawnX + 0.5, spawnY.toDouble(), spawnZ + 0.5)
        level.addFreshEntity(fishPool)
    }

    @JvmStatic
    fun tick(level: ServerLevel) {
        if (level.gameTime % SPAWN_INTERVAL == 0L) attemptSpawn(level)
    }

    private data class SpawnState(val lastValidSpawn: BlockPos?, val lastSpawnTime: Long)
}

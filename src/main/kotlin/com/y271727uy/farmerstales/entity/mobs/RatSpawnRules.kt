package com.y271727uy.farmerstales.entity.mobs

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.BlockTags
import net.minecraft.tags.StructureTags
import net.minecraft.world.entity.MobSpawnType
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Blocks
import net.minecraftforge.event.entity.EntityJoinLevelEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.WeakHashMap

/** Spawn constraints for rats, evaluated only while Minecraft is selecting natural mob spawn positions. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object RatSpawnRules {
    private const val SPAWN_RADIUS = 128
    private const val NEARBY_RAT_RADIUS = 24.0
    private const val CHUNK_COOLDOWN_TICKS = 12_000L
    private val lastNaturalSpawnByLevel = WeakHashMap<ServerLevel, MutableMap<Long, Long>>()

    fun canSpawn(level: ServerLevelAccessor, spawnType: MobSpawnType, pos: BlockPos): Boolean {
        if (spawnType !in NATURAL_SPAWN_TYPES) return true

        val serverLevel = level.level
        if (serverLevel.dimension() != Level.OVERWORLD) return false
        if (!hasSafeSurface(level, pos)) return false
        if (!isRatHabitat(serverLevel, pos)) return false
        if (hasNearbyRat(serverLevel, pos)) return false

        val lastSpawn = lastNaturalSpawnByLevel[serverLevel]?.get(ChunkPos(pos).toLong())
        return lastSpawn == null || serverLevel.gameTime >= lastSpawn + CHUNK_COOLDOWN_TICKS
    }

    @JvmStatic
    @SubscribeEvent
    fun onRatJoinLevel(event: EntityJoinLevelEvent) {
        val rat = event.entity as? RatEntity ?: return
        val level = event.level as? ServerLevel ?: return
        if (rat.spawnType !in NATURAL_SPAWN_TYPES) return

        lastNaturalSpawnByLevel.getOrPut(level, ::HashMap)[ChunkPos(rat.blockPosition()).toLong()] = level.gameTime
    }

    private fun hasSafeSurface(level: ServerLevelAccessor, pos: BlockPos): Boolean {
        val groundPos = pos.below()
        return level.getRawBrightness(pos, 0) > 8 &&
            level.getBlockState(groundPos).isFaceSturdy(level, groundPos, Direction.UP)
    }

    private fun isRatHabitat(level: ServerLevel, pos: BlockPos): Boolean =
        isInsideVillage(level, pos) || isNearWorldSpawn(level, pos) || hasCultivatedFieldNearby(level, pos)

    private fun isInsideVillage(level: ServerLevel, pos: BlockPos): Boolean =
        level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE).isValid

    private fun isNearWorldSpawn(level: ServerLevel, pos: BlockPos): Boolean {
        val spawn = level.sharedSpawnPos
        val x = pos.x - spawn.x
        val z = pos.z - spawn.z
        return x * x + z * z <= SPAWN_RADIUS * SPAWN_RADIUS
    }

    private fun hasCultivatedFieldNearby(level: ServerLevelAccessor, pos: BlockPos): Boolean {
        for (x in -4..4) {
            for (z in -4..4) {
                for (y in -1..1) {
                    val cropPos = pos.offset(x, y, z)
                    if (level.getBlockState(cropPos).`is`(BlockTags.CROPS) && level.getBlockState(cropPos.below()).`is`(Blocks.FARMLAND)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun hasNearbyRat(level: ServerLevel, pos: BlockPos): Boolean =
        level.getEntitiesOfClass(
            RatEntity::class.java,
            net.minecraft.world.phys.AABB(pos).inflate(NEARBY_RAT_RADIUS),
        ).isNotEmpty()

    // Chunk-generation spawn checks run on worker threads. The habitat scan can cross a chunk
    // boundary, which would make the generator wait on the chunk currently being generated.
    private val NATURAL_SPAWN_TYPES = setOf(MobSpawnType.NATURAL)
}

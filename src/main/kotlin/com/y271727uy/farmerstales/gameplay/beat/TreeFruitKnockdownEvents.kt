package com.y271727uy.farmerstales.gameplay.beat

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.registries.ForgeRegistries
import java.util.ArrayDeque

/** Knocks ripe fruit from compatible trees without depending on another mod's classes. */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object TreeFruitKnockdownEvents {
    private const val SEARCH_RADIUS = 7
    private const val MAX_SEARCHED_BLOCKS = 512
    private const val EXHAUSTION_COST = 1.0f
    private val RIPE_VALUES = setOf("FRUIT", "FRUITS", "BERRIES", "RIPE", "MATURE", "READY")

    @JvmStatic
    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (event.hand != InteractionHand.MAIN_HAND || event.itemStack.item != Items.STICK) return
        val level = event.level as? ServerLevel ?: return
        if (event.entity.foodData.foodLevel <= 0 || !isTreePart(level, event.pos)) return

        val harvested = knockDownRipeFruit(level, event.pos)
        if (!harvested) return

        event.setCancellationResult(InteractionResult.SUCCESS)
        event.isCanceled = true
        event.entity.foodData.addExhaustion(EXHAUSTION_COST)
        level.playSound(null, event.pos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.1f)
        level.sendParticles(
            ParticleTypes.SWEEP_ATTACK,
            event.entity.x, event.entity.y + 1.0, event.entity.z,
            1, 0.0, 0.0, 0.0, 0.0
        )
    }

    private fun knockDownRipeFruit(level: ServerLevel, origin: BlockPos): Boolean {
        val queue = ArrayDeque<BlockPos>()
        val visited = hashSetOf<BlockPos>()
        val fruitPositions = mutableListOf<BlockPos>()
        queue.add(origin)

        while (queue.isNotEmpty() && visited.size < MAX_SEARCHED_BLOCKS) {
            val pos = queue.removeFirst()
            if (!visited.add(pos) || !withinRadius(origin, pos)) continue
            val state = level.getBlockState(pos)
            if (!isTreePart(level, pos)) continue

            if (isFruitLeaves(state) && isRipe(state)) fruitPositions += pos
            for (direction in Direction.values()) {
                val next = pos.relative(direction)
                if (!visited.contains(next) && withinRadius(origin, next)) queue.addLast(next)
            }
        }

        var harvested = false
        for (pos in fruitPositions) {
            val state = level.getBlockState(pos)
            if (!isRipe(state)) continue
            val drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos))
            if (drops.isEmpty()) continue
            level.setBlock(pos, resetFruitState(state), 3)
            drops.filter { !it.isEmpty }.forEach { Block.popResource(level, pos, it) }
            harvested = true
        }
        return harvested
    }

    private fun isTreePart(level: ServerLevel, pos: BlockPos): Boolean {
        val key = ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).block) ?: return false
        val path = key.path.lowercase()
        return isFruitLeaves(level.getBlockState(pos)) ||
            path.contains("log") || path.contains("wood") || path.contains("stalk") || path.contains("stem")
    }

    private fun isFruitLeaves(state: BlockState): Boolean {
        val key = ForgeRegistries.BLOCKS.getKey(state.block) ?: return false
        return key.path.lowercase().contains("leaves") && key.namespace != "minecraft"
    }

    private fun isRipe(state: BlockState): Boolean {
        for (property in state.properties) {
            when (property) {
                is BooleanProperty -> if (property.name == "berries" || property.name == "fruiting") {
                    if (state.getValue(property)) return true
                }
                is IntegerProperty -> if (property.name == "age" || property.name == "blockstate") {
                    val max = property.possibleValues.maxOrNull() ?: continue
                    if (state.getValue(property) >= max) return true
                }
                else -> if (property.name == "type" && propertyValue(state, property).uppercase() in RIPE_VALUES) return true
            }
        }
        return false
    }

    private fun resetFruitState(state: BlockState): BlockState {
        var result = state
        for (property in state.properties) {
            when (property) {
                is BooleanProperty -> if (property.name == "berries" || property.name == "fruiting") {
                    result = result.setValue(property, false)
                }
                is IntegerProperty -> if (property.name == "age" || property.name == "blockstate") {
                    val reset = property.possibleValues.minOrNull() ?: continue
                    result = result.setValue(property, reset)
                }
                else -> if (property.name == "type") {
                    val reset = property.possibleValues.firstOrNull { it.toString().uppercase() == "LEAVES" }
                        ?: property.possibleValues.firstOrNull { it.toString().uppercase() == "FLOWERS" }
                    if (reset != null) {
                        @Suppress("UNCHECKED_CAST")
                        result = result.setValue(property as Property<Comparable<Any>>, reset as Comparable<Any>)
                    }
                }
            }
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun propertyValue(state: BlockState, property: Property<*>): String {
        return state.getValue(property as Property<Comparable<Any>>).toString()
    }

    private fun withinRadius(origin: BlockPos, pos: BlockPos): Boolean =
        kotlin.math.abs(pos.x - origin.x) <= SEARCH_RADIUS &&
            kotlin.math.abs(pos.y - origin.y) <= SEARCH_RADIUS &&
            kotlin.math.abs(pos.z - origin.z) <= SEARCH_RADIUS
}

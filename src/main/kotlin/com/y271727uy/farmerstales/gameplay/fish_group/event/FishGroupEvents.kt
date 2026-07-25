package com.y271727uy.farmerstales.gameplay.fish_group.event

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolLootManager
import com.y271727uy.farmerstales.gameplay.fish_group.spawn.FloatingPoolsSpawnerHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.ItemFishedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object FishGroupEvents {
    private const val HOOK_INTERACTION_HORIZONTAL_RANGE = 2.5
    private const val HOOK_INTERACTION_VERTICAL_RANGE = 4.5

    @JvmStatic
    @SubscribeEvent
    fun onLevelTick(event: TickEvent.LevelTickEvent) {
        val serverLevel = event.level as? ServerLevel ?: return
        if (event.phase != TickEvent.Phase.END) return
        FloatingPoolsSpawnerHelper.tick(serverLevel)
        serverLevel.players().forEach { player ->
            val hook = player.fishing?.takeUnless { it.isRemoved } ?: return@forEach
            findHookTarget(hook)?.markHookInteracting()
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onItemFished(event: ItemFishedEvent) {
        val hook = event.hookEntity ?: return
        if (hook.level().isClientSide) return
        val owner = hook.playerOwner ?: return
        findHookTarget(hook)?.let { entity ->
            entity.markHookInteracting()
            entity.onFishHookInteract(owner)
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(FishPoolLootManager)
        FTMod.LOGGER.info("Registered fish pool loot reload listener")
    }

    private fun findHookTarget(hookEntity: Entity): AbstractFishPoolEntity? {
        val hookPosition = hookEntity.position()
        val hookBox = hookEntity.boundingBox.inflate(
            HOOK_INTERACTION_HORIZONTAL_RANGE,
            HOOK_INTERACTION_VERTICAL_RANGE,
            HOOK_INTERACTION_HORIZONTAL_RANGE,
        )
        return hookEntity.level().getEntitiesOfClass(AbstractFishPoolEntity::class.java, hookBox)
            .asSequence()
            .filterNot { it.isRemoved }
            .filter { it.getHookInteractionBounds().intersects(hookBox) }
            .minByOrNull { it.getHookInteractionBounds().distanceToSqr(hookPosition) }
    }
}

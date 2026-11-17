package com.y271727uy.farmerstales.gameplay.season_fishing

import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.integration.IntegrationManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.storage.loot.BuiltInLootTables
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
object SeasonFishingLoot {
    @JvmStatic
    fun apply(player: ServerPlayer, rod: ItemStack, hook: FishingHook, drops: MutableList<ItemStack>) {
        if (!Config.restrictFishingLoot || !IntegrationManager.isSereneSeasonsLoaded()) {
            return
        }
        for (index in drops.lastIndex downTo 0) {
            val caught = drops[index]
            if (!SeasonalFishingTags.isManagedFish(caught.item)) {
                continue
            }

            when (val resolved = SeasonalFishingTags.resolveCatch(player.level(), player.random)) {
                is ResolvedFishingCatch.FISH -> drops[index] = ItemStack(resolved.item, caught.count)
                ResolvedFishingCatch.JUNK -> {
                    val junk = rollVanillaJunk(player, rod, hook)
                    if (junk.isNotEmpty()) {
                        drops.removeAt(index)
                        drops.addAll(index, junk)
                    }
                }
            }
        }
    }

    private fun rollVanillaJunk(player: ServerPlayer, rod: ItemStack, hook: FishingHook): List<ItemStack> {
        val level = player.serverLevel()
        val params = LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, hook.position())
            .withParameter(LootContextParams.TOOL, rod)
            .withParameter(LootContextParams.THIS_ENTITY, hook)
            .withOptionalParameter(LootContextParams.KILLER_ENTITY, hook.owner)
            .withLuck(player.luck)
            .create(LootContextParamSets.FISHING)
        return level.server.lootData.getLootTable(BuiltInLootTables.FISHING_JUNK).getRandomItems(params)
    }
}

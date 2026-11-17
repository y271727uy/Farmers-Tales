package com.y271727uy.farmerstales.gameplay.season_fishing

import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolLootManager
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.core.Holder
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.world.entity.projectile.FishingHook
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import java.util.WeakHashMap

object FocusedFishing {
    private val pendingCatches = WeakHashMap<ServerPlayer, PreviousCatch>()

    @JvmStatic
    fun takePreviousCatch(player: ServerPlayer): PreviousCatch? = pendingCatches.remove(player)

    @JvmStatic
    fun apply(player: ServerPlayer, hook: FishingHook, drops: MutableList<ItemStack>, previous: PreviousCatch?) {
        val level = player.serverLevel()
        val seasonsLoaded = IntegrationManager.isSereneSeasonsLoaded()
        val season = if (seasonsLoaded) SeasonSupport.currentSeasonWindow(level) else null
        val index = drops.indexOfFirst { !it.isEmpty && isEligibleFish(it.item, seasonsLoaded, season) }
        if (index < 0) return

        val dimension = level.dimension()
        val biome = level.getBiome(hook.blockPosition())
        val caught = drops[index]
        if (previous != null && previous.item != caught.item &&
            previous.dimension == dimension && previous.biome == biome &&
            isEligibleFish(previous.item, seasonsLoaded, season) && player.random.nextBoolean()
        ) {
            drops[index] = ItemStack(previous.item).apply {
                count = caught.count.coerceAtMost(maxStackSize)
            }
        }
        pendingCatches[player] = PreviousCatch(drops[index].item, dimension, biome)
    }

    private fun isEligibleFish(item: Item, seasonsLoaded: Boolean, season: SeasonWindow?): Boolean {
        val seasons = SeasonalFishingTags.catchSeasons(item).ifEmpty { SeasonalFishingTags.activeSeasons(item) }
        if (seasons.isEmpty() && !item.builtInRegistryHolder().`is`(ItemTags.FISHES)) return false
        if (seasonsLoaded && seasons.isNotEmpty() && season !in seasons) return false
        return !FishPoolLootManager.isFishKing(item)
    }

    data class PreviousCatch(val item: Item, val dimension: ResourceKey<Level>, val biome: Holder<Biome>)
}

package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.entity.AbstractFishPoolEntity
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import snownee.jade.api.EntityAccessor
import snownee.jade.api.IEntityComponentProvider
import snownee.jade.api.IServerDataProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object FishPoolEntityProvider : IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    private val UID = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "fish_pool")

    override fun getUid(): ResourceLocation = UID

    override fun appendServerData(tag: CompoundTag, accessor: EntityAccessor) {
        val fishPool = accessor.entity as? AbstractFishPoolEntity ?: return
        val serverLevel = accessor.level as? ServerLevel ?: return
        fishPool.appendJadeServerData(tag, serverLevel)
    }

    override fun appendTooltip(tooltip: ITooltip, accessor: EntityAccessor, config: IPluginConfig) {
        val data = accessor.serverData
        if (data.isEmpty) {
            return
        }

        tooltip.add(
            Component.translatable(
                "tooltip.${FTMod.MODID}.fish_pool.fish_king",
                Component.translatable(data.getString("fishKingKey")),
            ),
        )
        tooltip.add(
            Component.translatable(
                "tooltip.${FTMod.MODID}.fish_pool.weather",
                Component.translatable(data.getString("weatherKey")),
            ),
        )
        tooltip.add(
            Component.translatable(
                "tooltip.${FTMod.MODID}.fish_pool.time",
                Component.translatable(data.getString("timeKey")),
            ),
        )
        tooltip.add(
            Component.translatable(
                "tooltip.${FTMod.MODID}.fish_pool.state",
                Component.translatable(data.getString("stateKey")),
            ),
        )
    }
}

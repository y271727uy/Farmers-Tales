package com.y271727uy.farmerstales.gameplay.advancements

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.mobs.ArmadilloEntity
import com.y271727uy.farmerstales.entity.mobs.NautilusEntity
import com.y271727uy.farmerstales.entity.mobs.ZombieNautilusEntity
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
object DiscoveryAdvancementEvents {
    @JvmStatic
    @SubscribeEvent
    fun onStartTracking(event: PlayerEvent.StartTracking) {
        val player = event.entity as? ServerPlayer ?: return
        when (event.target) {
            is ArmadilloEntity -> FarmersTalesAdvancements.award(player, "discovery/armadillo")
            is ZombieNautilusEntity -> FarmersTalesAdvancements.award(player, "discovery/zombie_nautilus")
            is NautilusEntity -> FarmersTalesAdvancements.award(player, "discovery/nautilus")
        }
    }
}

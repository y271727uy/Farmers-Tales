package com.y271727uy.farmerstales.gameplay.fish_group.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.OceanFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.RiverFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.item.FishGroupRegistry
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.FishingRodItem
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object FishGroupClientEvents {
    @JvmStatic
    @SubscribeEvent
    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            ItemProperties.register(
                FishGroupRegistry.BAMBOO_FISHING_ROD.get(),
                ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "cast"),
            ) { stack, _, entity, _ ->
                val player = entity as? Player ?: return@register 0.0F
                val mainHand = player.mainHandItem === stack
                var offHand = player.offhandItem === stack
                if (player.mainHandItem.item is FishingRodItem) offHand = false
                if ((mainHand || offHand) && player.fishing != null) 1.0F else 0.0F
            }
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        FishGroupRegistry.getFishPoolRegistrations().forEach { registration ->
            registerFishPoolRenderer(event, registration)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerFishPoolRenderer(
        event: EntityRenderersEvent.RegisterRenderers,
        registration: FishGroupRegistry.FishPoolRegistration,
    ) {
        when (registration.environment) {
            FishPoolDefinition.Environment.RIVER -> event.registerEntityRenderer(
                registration.entityType.get() as EntityType<RiverFishPoolEntity>,
                EntityRendererProvider(::RiverFishPoolRenderer),
            )

            FishPoolDefinition.Environment.OCEAN -> event.registerEntityRenderer(
                registration.entityType.get() as EntityType<OceanFishPoolEntity>,
                EntityRendererProvider(::OceanFishPoolRenderer),
            )
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun onRegisterLayerDefinitions(event: EntityRenderersEvent.RegisterLayerDefinitions) {
        event.registerLayerDefinition(RiverFishPoolModel.LAYER_LOCATION) { RiverFishPoolModel.getTexturedModelData() }
        event.registerLayerDefinition(OceanFishPoolModel.LAYER_LOCATION) { OceanFishPoolModel.getTexturedModelData() }
    }
}

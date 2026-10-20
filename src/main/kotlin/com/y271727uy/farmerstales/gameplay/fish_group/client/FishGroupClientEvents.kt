package com.y271727uy.farmerstales.gameplay.fish_group.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.OceanFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.client.model.RiverFishPoolModel
import com.y271727uy.farmerstales.gameplay.fish_group.entity.OceanFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.entity.RiverFishPoolEntity
import com.y271727uy.farmerstales.gameplay.fish_group.item.FishGroupRegistry
import com.y271727uy.farmerstales.gameplay.fish_group.pool.FishPoolDefinition
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.entity.EntityType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.event.RegisterColorHandlersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object FishGroupClientEvents {
    @JvmStatic
    @SubscribeEvent
    fun onRegisterItemColors(event: RegisterColorHandlersEvent.Item) {
        event.register(
            { _, tintIndex -> if (tintIndex == 0) EGG_BASE_COLOR else EGG_SPOT_COLOR },
            *FishGroupRegistry.getFishPoolRegistrations().map { it.item.get() }.toTypedArray(),
        )
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

    private const val EGG_BASE_COLOR = 0x1B6E8C
    private const val EGG_SPOT_COLOR = 0x9CE7F7
}

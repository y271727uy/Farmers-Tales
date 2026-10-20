package com.y271727uy.farmerstales

import com.mojang.logging.LogUtils
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.all.ModBlockEntities
import com.y271727uy.farmerstales.all.ModCreativeModeTabs
import com.y271727uy.farmerstales.all.ModEnchantments
import com.y271727uy.farmerstales.all.ModItems
import com.y271727uy.farmerstales.all.ModLootModifiers
import com.y271727uy.farmerstales.all.ModSounds
import com.y271727uy.farmerstales.entity.ModEntities
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.data.ModDataGen
import com.y271727uy.farmerstales.gameplay.fish_group.item.FishGroupRegistry
import com.y271727uy.farmerstales.gameplay.food_expiry.FoodExpiryCapabilities
import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonEffects
import com.y271727uy.farmerstales.gameplay.curated_crop.CuratedCropParticles
import com.y271727uy.farmerstales.gameplay.special_event.fertile_moon.FertileMoonEffects
import com.y271727uy.farmerstales.network.FarmerstalesNetwork
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.slf4j.Logger

@Mod(FTMod.MODID)
class FTMod {
    init {
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        ModItems.init()
        ModSounds.register(modEventBus)
        ModEnchantments.register(modEventBus)
        ModLootModifiers.register(modEventBus)
        ModBlocks.init()
        ModBlockEntities.init()
        ModEntities.register(modEventBus)
        FishGroupRegistry.register(modEventBus)
        FertileMoonEffects.register(modEventBus)
        CrimsonMoonEffects.register(modEventBus)
        CuratedCropParticles.register(modEventBus)
        ModDataGen.init()
        ModCreativeModeTabs.register(modEventBus)
        modEventBus.addListener<RegisterCapabilitiesEvent>(FoodExpiryCapabilities::register)
        modEventBus.addListener(::commonSetup)
        MinecraftForge.EVENT_BUS.register(this)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        FarmerstalesNetwork.register()
        event.enqueueWork(ModEntities::registerSpawnPlacements)
        IntegrationManager.init()
        LOGGER.info("Farmer's Tales common setup complete")
    }

    companion object {
        const val MODID = "farmerstales"

        @JvmField
        val LOGGER: Logger = LogUtils.getLogger()
    }
}

package com.y271727uy.farmerstales

import com.mojang.logging.LogUtils
import com.y271727uy.farmerstales.all.ModBlocks
import com.y271727uy.farmerstales.all.ModBlockEntities
import com.y271727uy.farmerstales.all.ModItems
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.config.Config
import com.y271727uy.farmerstales.data.ModDataGen
import com.y271727uy.farmerstales.gameplay.fish_group.item.FishGroupRegistry
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import org.slf4j.Logger

@Mod(FTMod.MODID)
class FTMod {
    init {
        val modEventBus = FMLJavaModLoadingContext.get().modEventBus

        ModItems.init()
        ModBlocks.init()
        ModBlockEntities.init()
        FishGroupRegistry.register(modEventBus)
        ModDataGen.init()
        IntegrationManager.initCreativeTabs(modEventBus)
        modEventBus.addListener(::commonSetup)
        MinecraftForge.EVENT_BUS.register(this)
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        IntegrationManager.init()
        LOGGER.info("Farmer's Tales common setup complete")
    }

    companion object {
        const val MODID = "farmerstales"

        @JvmField
        val LOGGER: Logger = LogUtils.getLogger()
    }
}

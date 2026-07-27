package com.y271727uy.farmerstales.data

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.entity.data.ModEntityBiomeModifierProvider
import com.y271727uy.farmerstales.entity.data.ModEntityLootTableProvider
import com.y271727uy.farmerstales.registrate.ModRegistrate
import com.tterrag.registrate.providers.ProviderType
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraft.data.loot.LootTableProvider
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModDataGen {
    @JvmStatic
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        event.generator.addProvider(event.includeServer(), ModEntityBiomeModifierProvider(event.generator.packOutput))
        event.generator.addProvider(
            event.includeServer(),
            LootTableProvider(
                event.generator.packOutput,
                emptySet(),
                listOf(LootTableProvider.SubProviderEntry(::ModEntityLootTableProvider, LootContextParamSets.ENTITY)),
            ),
        )
    }

    fun init() {
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG) { provider ->
            ModLangs.init(provider)
        }
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.ITEM_MODEL) { provider ->
            ModModels.initItem(provider)
        }
    }
}

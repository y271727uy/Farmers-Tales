package com.y271727uy.farmerstales.all

import com.y271727uy.farmerstales.FTMod
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject

object ModCreativeModeTabs {
    private val tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FTMod.MODID)

    @JvmField
    val FARMERS_TALES: RegistryObject<CreativeModeTab> = tabs.register("farmerstales") {
        CreativeModeTab.builder()
            .icon { ItemStack(ModItems.TREE_COMPOST.get()) }
            .title(Component.translatable("itemGroup.${FTMod.MODID}"))
            .displayItems { _, output ->
                BuiltInRegistries.ITEM.forEach { item ->
                    if (BuiltInRegistries.ITEM.getKey(item).namespace == FTMod.MODID) {
                        output.accept(item)
                    }
                }
            }
            .build()
    }

    fun register(bus: IEventBus) {
        tabs.register(bus)
    }
}

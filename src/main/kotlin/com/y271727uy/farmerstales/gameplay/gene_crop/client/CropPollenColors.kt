package com.y271727uy.farmerstales.gameplay.gene_crop.client

import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.items.CropPollenItem
import com.y271727uy.farmerstales.all.items.GeneCropItems
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenDefinitions
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.SimplePreparableReloadListener
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RegisterColorHandlersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.io.IOException

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object CropPollenColors : SimplePreparableReloadListener<Map<ResourceLocation, Int>>() {
    private val palette = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "pollen_colors.json")
    private val hexColor = Regex("#[0-9a-fA-F]{6}")
    private const val DEFAULT_COLOR = 0xF2D47A
    private const val UNTINTED = -1

    private var colors: Map<ResourceLocation, Int> = emptyMap()

    @JvmStatic
    @SubscribeEvent
    fun registerReloadListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(this)
    }

    @JvmStatic
    @SubscribeEvent
    fun registerItemColor(event: RegisterColorHandlersEvent.Item) {
        event.register(::color, GeneCropItems.CROP_POLLEN.get())
    }

    private fun color(stack: ItemStack, tintIndex: Int): Int {
        if (tintIndex != 0) return UNTINTED
        val seedId = CropPollenItem.seedId(stack) ?: return UNTINTED
        val seed = BuiltInRegistries.ITEM.getOptional(seedId).orElse(null) ?: return UNTINTED
        if (!CropPollenDefinitions.supports(seed)) return UNTINTED
        return colors[seedId] ?: DEFAULT_COLOR
    }

    override fun prepare(manager: ResourceManager, profiler: ProfilerFiller): Map<ResourceLocation, Int> = buildMap {
        for (resource in manager.getResourceStack(palette)) {
            val json = try {
                resource.openAsReader().use { JsonParser.parseReader(it) }
            } catch (exception: IOException) {
                FTMod.LOGGER.warn("Could not read {} from {}", palette, resource.sourcePackId(), exception)
                continue
            } catch (exception: JsonParseException) {
                FTMod.LOGGER.warn("Could not parse {} from {}", palette, resource.sourcePackId(), exception)
                continue
            }
            if (!json.isJsonObject) {
                FTMod.LOGGER.warn("Expected an object in {} from {}", palette, resource.sourcePackId())
                continue
            }
            for ((key, value) in json.asJsonObject.entrySet()) {
                val seedId = ResourceLocation.tryParse(key)
                val hex = value.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString
                if (seedId == null || hex == null || !hexColor.matches(hex)) {
                    FTMod.LOGGER.warn("Invalid pollen color for '{}' in {} from {}", key, palette, resource.sourcePackId())
                    continue
                }
                put(seedId, hex.substring(1).toInt(16))
            }
        }
    }

    override fun apply(data: Map<ResourceLocation, Int>, manager: ResourceManager, profiler: ProfilerFiller) {
        colors = data
    }
}

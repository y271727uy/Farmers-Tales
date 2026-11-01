package com.y271727uy.farmerstales.gameplay.clock.client

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.IntegrationManager
import com.y271727uy.farmerstales.mixin.minecraft.client.BiomeTemperatureAccessor
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent
import net.minecraftforge.client.gui.overlay.IGuiOverlay
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.Locale
import kotlin.math.roundToInt

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ClockHud {
    private const val MARGIN = 6
    private const val PADDING = 3
    private const val ICON_SIZE = 16
    private const val GAP = 4
    private val clock = ItemStack(Items.CLOCK)
    private var minimapDisplayRegistered = false
    private val environmentIconStyle = Style.EMPTY
        .withFont(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "clock"))
        .withColor(0xFFFFFF)

    fun environmentLines(level: Level, pos: BlockPos): List<Component> {
        val holder = level.getBiome(pos)
        val biome = holder.value()
        val biomeName = holder.unwrapKey().map<Component> {
            Component.translatable(it.location().toLanguageKey("biome"))
        }.orElseGet { Component.translatable("hud.${FTMod.MODID}.unknown_biome") }
        val fahrenheit = (biome as BiomeTemperatureAccessor).`farmerstales$getTemperature`(pos) * 100.0
        val celsius = (fahrenheit - 32.0) / 1.8
        val humidity = (biome.modifiedClimateSettings.downfall() * 100.0F).roundToInt()
        return listOf(
            environmentLine('\uE101', Component.translatable("hud.${FTMod.MODID}.biome", biomeName)),
            environmentLine('\uE102', Component.translatable(
                "hud.${FTMod.MODID}.temperature", String.format(Locale.ROOT, "%.2f", fahrenheit),
                celsius.roundToInt(), (celsius + 273.15).roundToInt()
            )),
            environmentLine('\uE103', Component.translatable("hud.${FTMod.MODID}.humidity", humidity))
        )
    }

    private fun environmentLine(icon: Char, text: Component): Component = Component.empty()
        .append(Component.literal(icon.toString()).setStyle(environmentIconStyle)).append(" ").append(text)

    data class CalendarLines(val date: Component, val time: Component)

    fun calendarLines(level: Level): CalendarLines {
        val dayTime = level.dayTime
        val day = Math.floorDiv(dayTime, 24000L) + 1L
        val calendar = IntegrationManager.calendarAt(level, 0L)
        val ticksPerYear = (calendar?.dayDuration?.toLong() ?: 24000L) *
            (calendar?.daysInMonth?.toLong() ?: 8L) * 12L
        val year = 1L + Math.floorDiv(dayTime, ticksPerYear)
        val clockTicks = (Math.floorMod(dayTime, 24000L) + 6000L) % 24000L
        val hour = (clockTicks / 1000L).toString().padStart(2, '0')
        val minute = (clockTicks % 1000L * 60L / 1000L).toString().padStart(2, '0')
        return CalendarLines(
            Component.translatable("hud.${FTMod.MODID}.year_and_day", year, day),
            Component.literal("$hour:$minute")
        )
    }

    @JvmStatic
    fun onMinimapDisplayRegistered() {
        minimapDisplayRegistered = true
    }

    private val overlay = IGuiOverlay { _, graphics, _, screenWidth, screenHeight ->
        val minecraft = Minecraft.getInstance()
        val level = minecraft.level
        val player = minecraft.player
        if (level != null && player != null && !minimapDisplayRegistered) {
            val lines = calendarLines(level)
            val environment = environmentLines(level, player.blockPosition())
            val font = minecraft.font
            val dateWidth = ICON_SIZE + GAP + font.width(lines.date)
            val contentWidth = maxOf(dateWidth, font.width(lines.time), environment.maxOf { font.width(it) })
            val width = PADDING * 2 + contentWidth
            val environmentHeight = environment.size * (font.lineHeight + 1)
            val height = environmentHeight + ICON_SIZE + 2 + font.lineHeight + PADDING * 2
            val x = (screenWidth - MARGIN - width).coerceAtLeast(0)
            val y = (screenHeight - MARGIN - height).coerceAtLeast(0)

            graphics.fill(x, y, x + width, y + height, 0x80000000.toInt())
            environment.forEachIndexed { index, text ->
                graphics.drawString(font, text, x + PADDING, y + PADDING + index * (font.lineHeight + 1), 0xFFFFFF, true)
            }
            val dateY = y + PADDING + environmentHeight
            graphics.renderItem(clock, x + PADDING, dateY)
            graphics.drawString(
                font, lines.date, x + PADDING + ICON_SIZE + GAP,
                dateY + (ICON_SIZE - font.lineHeight) / 2, 0xFFFFFF, true
            )
            graphics.drawString(
                font, lines.time, x + PADDING + (dateWidth - font.width(lines.time)) / 2,
                dateY + ICON_SIZE + 2, 0xFFFFFF, true
            )
        }
    }

    @JvmStatic
    @SubscribeEvent
    fun registerOverlays(event: RegisterGuiOverlaysEvent) {
        event.registerAboveAll("clock", overlay)
    }
}

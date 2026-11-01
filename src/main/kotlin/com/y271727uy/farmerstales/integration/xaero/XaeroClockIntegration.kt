package com.y271727uy.farmerstales.integration.xaero

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.clock.client.ClockHud
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import xaero.hud.minimap.info.InfoDisplay
import xaero.hud.minimap.info.widget.InfoDisplayCommonWidgetFactories
import xaero.lib.common.config.option.value.io.serialization.BuiltInConfigValueIOCodecs

/** Loaded only by the optional, client-only Xaero mixin. */
object XaeroClockIntegration {
    // Xaero's legacy settings format uses ':' as a delimiter in info-display IDs.
    private const val DISPLAY_ID = "${FTMod.MODID}_clock"
    private val clock = ItemStack(Items.CLOCK)
    private val timeProperty = ResourceLocation.withDefaultNamespace("time")
    private val iconStyle = Style.EMPTY
        .withFont(ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "clock"))
        .withColor(0xFFFFFF)

    /** Xaero reverses the vertical direction when the minimap is near the bottom. */
    @JvmStatic
    fun orderForRendering(displays: Iterator<InfoDisplay<*>>, belowMap: Boolean): Iterator<InfoDisplay<*>> {
        val ordered = displays.asSequence().toMutableList()
        val clockDisplay = ordered.firstOrNull { it.id == DISPLAY_ID }
        val seasonDisplay = ordered.firstOrNull { it.id == "season" }
        if (clockDisplay != null && seasonDisplay != null) {
            ordered.remove(clockDisplay)
            val seasonIndex = ordered.indexOf(seasonDisplay)
            ordered.add(seasonIndex + if (belowMap) 1 else 0, clockDisplay)
        }
        return ordered.iterator()
    }

    @JvmStatic
    fun register(displays: MutableList<InfoDisplay<*>>) {
        InfoDisplay.Builder.begin<Boolean>()
            .setId(DISPLAY_ID)
            .setName(Component.translatable("hud.${FTMod.MODID}.clock"))
            .setDefaultState(true)
            .setCodec(BuiltInConfigValueIOCodecs.BOOLEAN)
            .setWidgetFactory(InfoDisplayCommonWidgetFactories.OFF_ON)
            .setCompiler { display, compiler, _, _, _ ->
                val minecraft = Minecraft.getInstance()
                val level = minecraft.level
                val player = minecraft.player
                if (display.effectiveState && level != null && player != null) {
                    ClockHud.environmentLines(level, player.blockPosition()).forEach { compiler.addLine(it) }
                    val time = ItemProperties.getProperty(Items.CLOCK, timeProperty)
                        ?.call(clock, level, player, 0) ?: 0.0F
                    // Match vanilla clock.json's half-frame thresholds and wraparound.
                    val frame = Math.floorMod(kotlin.math.floor(time * 64.0F + 0.5F).toInt(), 64)
                    val icon = Component.literal((0xE000 + frame).toChar().toString())
                        .setStyle(iconStyle)
                    val lines = ClockHud.calendarLines(level)
                    val dateLine = Component.empty().append(icon).append(" ").append(lines.date)
                    compiler.addLine(dateLine)
                    // Give both lines the same width so the time stays centered
                    // under the date for every Xaero information-text alignment.
                    val padding = (minecraft.font.width(dateLine) - minecraft.font.width(lines.time))
                        .coerceAtLeast(0)
                    val leftPadding = Component.literal("\uE100".repeat(padding / 2)).setStyle(iconStyle)
                    val rightPadding = Component.literal("\uE100".repeat(padding - padding / 2)).setStyle(iconStyle)
                    compiler.addLine(Component.empty().append(leftPadding).append(lines.time).append(rightPadding))
                }
            }
            // Append our own row; leave SeasonHUD's "season" entry and ordering intact.
            .setDestination { displays.add(it) }
            .build()
        ClockHud.onMinimapDisplayRegistered()
    }
}

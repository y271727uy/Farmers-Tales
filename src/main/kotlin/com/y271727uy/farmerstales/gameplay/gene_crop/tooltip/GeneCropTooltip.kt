package com.y271727uy.farmerstales.gameplay.gene_crop.tooltip

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.world.item.ItemStack
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * Gene lines follow [forestry.arboriculture.TreeSpecies.addTooltip]:
 * a letter, a colon, the allele name, two per row, the whole segment one colour.
 * Saplings are yellow `S:`, maturation red `M:`, height light purple `H:`,
 * girth aqua `G:`, yield white `Y:`, sappiness gold `S:`.
 * Crops keep the two matching letters and add temperature and pest resistance.
 * No genome NBT still shows Forestry's gray `for.gui.unknown` line.
 */
@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
@Suppress("unused")
object GeneCropTooltip {
    private const val UNKNOWN_KEY = "for.gui.unknown"
    private const val UNKNOWN_ARGUMENT = "< %s >"
    private const val DETAILS_KEY = "for.gui.tooltip.tmi"

    private data class Stat(val letter: String, val value: Component, val color: ChatFormatting)

    @JvmStatic
    fun unknownLine(): MutableComponent =
        Component.translatable(UNKNOWN_KEY, UNKNOWN_ARGUMENT).withStyle(ChatFormatting.GRAY)

    @JvmStatic
    fun appliesTo(stack: ItemStack): Boolean = GeneCropGenome.isGeneSeed(stack)

    @JvmStatic
    fun lines(stack: ItemStack): List<Component> {
        val stats = ArrayList<Stat>(4)
        GeneCropGenome.growthName(stack)?.let { stats += Stat("M", it, ChatFormatting.RED) }
        GeneCropGenome.yieldName(stack)?.let { stats += Stat("Y", it, ChatFormatting.WHITE) }
        GeneCropGenome.temperatureName(stack)?.let { stats += Stat("T", it, ChatFormatting.GREEN) }
        GeneCropGenome.resistanceName(stack)?.let { stats += Stat("R", it, ChatFormatting.DARK_GREEN) }
        if (stats.isEmpty()) {
            return listOf(unknownLine())
        }
        if (!Screen.hasShiftDown()) {
            return listOf(detailsLine())
        }
        return rows(stats)
    }

    /** Forestry styles the collapsed line gray and italic, without changing the font. */
    private fun detailsLine(): MutableComponent =
        Component.translatable(DETAILS_KEY).withStyle { style: Style ->
            style.applyFormat(ChatFormatting.GRAY).withItalic(true)
        }

    @JvmStatic
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onItemTooltip(event: ItemTooltipEvent) {
        if (!appliesTo(event.itemStack)) {
            return
        }
        val tooltip = event.toolTip
        var index = if (tooltip.isEmpty()) 0 else 1
        for (line in lines(event.itemStack)) {
            tooltip.add(index, line)
            index++
        }
    }

    /** Same join as Forestry: left segment keeps the comma, right segment does not. */
    private fun rows(stats: List<Stat>): List<Component> {
        val lines = ArrayList<Component>((stats.size + 1) / 2)
        var index = 0
        while (index < stats.size) {
            val left = stats[index]
            val right = stats.getOrNull(index + 1)
            val line = segment(left, trailingComma = right != null)
            if (right != null) {
                line.append(segment(right, trailingComma = false))
            }
            lines += line
            index += 2
        }
        return lines
    }

    private fun segment(stat: Stat, trailingComma: Boolean): MutableComponent {
        val text = Component.literal("${stat.letter}:").append(stat.value)
        if (trailingComma) {
            text.append(",")
        }
        return text.withStyle(stat.color)
    }
}

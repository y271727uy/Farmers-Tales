package com.y271727uy.farmerstales.gameplay.curated_crop.tooltip

import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.util.Mth

/** Simple flowing rainbow text, with no custom renderer or extra dependencies. */
object RainbowText {
    /**
     * Rebuild this component when generating the tooltip so its colors keep moving.
     * [periodMillis] controls the duration of one color cycle; twelve characters span a rainbow.
     * For translated text, pass Component.translatable(key).string after resolving the language.
     */
    @JvmStatic
    @JvmOverloads
    fun rainbow(text: String, periodMillis: Long = 3_000L): MutableComponent {
        require(periodMillis > 0) { "periodMillis must be positive" }

        val result = Component.empty()
        val phase = (Util.getMillis() % periodMillis).toDouble() / periodMillis
        var offset = 0
        var index = 0
        while (offset < text.length) {
            val next = offset + Character.charCount(text.codePointAt(offset))
            val hue = ((phase + index / 12.0) % 1.0).toFloat()
            val color = Mth.hsvToRgb(hue, 0.8f, 1.0f)
            result.append(
                Component.literal(text.substring(offset, next))
                    .withStyle { style -> style.withColor(color) },
            )
            offset = next
            index++
        }
        return result
    }
}

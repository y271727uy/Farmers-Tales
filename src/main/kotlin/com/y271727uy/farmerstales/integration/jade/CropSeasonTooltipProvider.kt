package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport
import com.y271727uy.farmerstales.integration.sereneseasons.SeasonSupport.SeasonWindow
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import sereneseasons.init.ModTags
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

object CropSeasonTooltipProvider : IBlockComponentProvider {
    private val uid = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "crop_seasons")
    private val seasonTags = mapOf(
        SeasonWindow.SPRING to ModTags.Blocks.SPRING_CROPS,
        SeasonWindow.SUMMER to ModTags.Blocks.SUMMER_CROPS,
        SeasonWindow.AUTUMN to ModTags.Blocks.AUTUMN_CROPS,
        SeasonWindow.WINTER to ModTags.Blocks.WINTER_CROPS,
    )

    override fun getUid(): ResourceLocation = uid

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val state = accessor.blockState
        val seasons = if (state.`is`(ModTags.Blocks.YEAR_ROUND_CROPS)) {
            SeasonWindow.entries
        } else {
            seasonTags.mapNotNull { (season, tag) -> season.takeIf { state.`is`(tag) } }
        }
        if (seasons.isEmpty()) return

        tooltip.add(Component.translatable(
            "tooltip.${FTMod.MODID}.crop_seasons",
            SeasonSupport.formatSeasonsInlineOrYearRound(seasons),
        ))
    }
}

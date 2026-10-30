package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/** One green line when the looked-at gene crop has an identified genome. Nothing otherwise. */
object GeneCropTooltipProvider : IBlockComponentProvider {
    private val UID = ResourceLocation.fromNamespaceAndPath(FTMod.MODID, "gene_crop")

    override fun getUid(): ResourceLocation = UID

    override fun appendTooltip(tooltip: ITooltip, accessor: BlockAccessor, config: IPluginConfig) {
        val pos = accessor.position
        if (!GeneCropGenome.isGeneCrop(accessor.blockState, accessor.level, pos)) {
            return
        }
        GeneCropJadeCache.prepare(accessor.level)
        if (GeneCropJadeCache.identified(pos) == true) {
            tooltip.add(
                Component.translatable("tooltip.${FTMod.MODID}.jade.gene_crop.identified")
                    .withStyle(ChatFormatting.GREEN),
            )
        }
        GeneCropJadeCache.request(pos)
    }
}

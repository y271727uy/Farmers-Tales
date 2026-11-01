package com.y271727uy.farmerstales.integration.jade

import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome
import com.y271727uy.farmerstales.gameplay.gene_crop.CropTemperatureStatus
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import snownee.jade.api.BlockAccessor
import snownee.jade.api.IBlockComponentProvider
import snownee.jade.api.ITooltip
import snownee.jade.api.config.IPluginConfig

/** Genetics and the saved pollination state of the looked-at stand. */
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
        val stand = accessor.level.getBlockEntity(pos) as? CropStandBlockEntity ?: return
        if (!stand.isMature()) GeneCropJadeCache.temperature(pos)?.let { status ->
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.gene_crop.temperature.${status.translationSuffix}")
                .withStyle(when (status) {
                    CropTemperatureStatus.SUITABLE -> ChatFormatting.GREEN
                    CropTemperatureStatus.CHILLY, CropTemperatureStatus.WARM -> ChatFormatting.YELLOW
                    else -> ChatFormatting.RED
                }))
        }
        if (stand.pollenFather() != null) {
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.jade.gene_crop.pollinated")
                .withStyle(ChatFormatting.GOLD))
        } else if (stand.isMature()) {
            tooltip.add(Component.translatable("tooltip.${FTMod.MODID}.jade.gene_crop.awaiting_pollen")
                .withStyle(ChatFormatting.GRAY))
        }
    }
}

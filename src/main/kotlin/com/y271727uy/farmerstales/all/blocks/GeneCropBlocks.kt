package com.y271727uy.farmerstales.all.blocks

import com.tterrag.registrate.providers.ProviderType
import com.tterrag.registrate.util.entry.BlockEntry
import com.y271727uy.farmerstales.gameplay.gene_crop.block.CropStandBlock
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockBehaviour

object GeneCropBlocks {
    @JvmField
    val CROP_STAND: BlockEntry<CropStandBlock> = ModRegistrate.REGISTRATE
        .block("crop_stand") { _ ->
            CropStandBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(1.0f).noCollission().noOcclusion().randomTicks())
        }
        .blockstate { context, provider ->
            provider.simpleBlock(context.entry, provider.models().getExistingFile(provider.modLoc("block/crop_stand")))
        }
        .setData(ProviderType.LANG) { _, _ -> }
        .register()

    fun init() = Unit
}

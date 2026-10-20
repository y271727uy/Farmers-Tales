package com.y271727uy.farmerstales.all.blocks

import com.tterrag.registrate.providers.ProviderType
import com.tterrag.registrate.util.entry.BlockEntry
import com.y271727uy.farmerstales.gameplay.animal_husbandry.block.AnimalDungBlock
import com.y271727uy.farmerstales.registrate.ModRegistrate
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour

object AnimalHusbandryBlocks {
    @JvmField
    val ANIMAL_DUNG: BlockEntry<AnimalDungBlock> = ModRegistrate.REGISTRATE
        .block("animal_dung") { properties ->
            AnimalDungBlock(
                BlockBehaviour.Properties.copy(Blocks.BROWN_MUSHROOM)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .sound(SoundType.GRAVEL)
            )
        }
        .blockstate { context, provider ->
            provider.simpleBlock(
                context.entry,
                provider.models().getExistingFile(provider.modLoc("block/shitblock"))
            )
        }
        .setData(ProviderType.LANG) { _, _ -> }
        .register()

    fun init() = Unit
}

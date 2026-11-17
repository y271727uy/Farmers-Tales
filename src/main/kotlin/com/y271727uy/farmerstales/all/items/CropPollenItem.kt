package com.y271727uy.farmerstales.all.items

import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import com.y271727uy.farmerstales.gameplay.gene_crop.pollen.CropPollenSample
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level

class CropPollenItem(properties: Properties) : Item(properties) {
    override fun getName(stack: ItemStack): Component {
        val seed = sample(stack)?.seed() ?: return super.getName(stack)
        if (seed.isEmpty) return super.getName(stack)
        return Component.translatable("item.farmerstales.crop_pollen.named", seed.hoverName)
    }

    override fun appendHoverText(stack: ItemStack, level: Level?, tooltip: MutableList<Component>, flag: TooltipFlag) {
        if (sample(stack)?.isViable() == true) {
            tooltip.add(Component.translatable("tooltip.farmerstales.crop_pollen.ready").withStyle(ChatFormatting.GRAY))
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val sample = sample(context.itemInHand) ?: return InteractionResult.PASS
        if (!sample.isViable()) return InteractionResult.PASS
        if (sample.source == context.clickedPos && sample.dimension == context.level.dimension().location()) return InteractionResult.PASS
        val stand = context.level.getBlockEntity(context.clickedPos) as? CropStandBlockEntity ?: return InteractionResult.PASS
        if (!stand.canAcceptPollen(sample.seed())) return InteractionResult.PASS
        if (!context.level.isClientSide && stand.acceptPollen(sample.seed())) {
            if (context.player?.abilities?.instabuild != true) context.itemInHand.shrink(1)
        }
        return InteractionResult.sidedSuccess(context.level.isClientSide)
    }

    companion object {
        private const val SAMPLE = "CropPollen"
        @JvmStatic
        fun create(sample: CropPollenSample): ItemStack = ItemStack(GeneCropItems.CROP_POLLEN.get()).also {
            it.getOrCreateTag().put(SAMPLE, sample.write())
        }

        @JvmStatic
        fun sample(stack: ItemStack): CropPollenSample? = sampleTag(stack)?.let(CropPollenSample::read)

        fun seedId(stack: ItemStack): ResourceLocation? = sampleTag(stack)?.let(CropPollenSample::readSeedId)

        private fun sampleTag(stack: ItemStack): CompoundTag? {
            if (stack.item !is CropPollenItem) return null
            val tag = stack.tag ?: return null
            return if (tag.contains(SAMPLE, Tag.TAG_COMPOUND.toInt())) tag.getCompound(SAMPLE) else null
        }
    }
}

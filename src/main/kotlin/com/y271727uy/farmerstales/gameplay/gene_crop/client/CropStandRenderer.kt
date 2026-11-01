package com.y271727uy.farmerstales.gameplay.gene_crop.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.all.ModBlockEntities
import com.y271727uy.farmerstales.gameplay.gene_crop.block.entity.CropStandBlockEntity
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

class CropStandRenderer(private val context: BlockEntityRendererProvider.Context) : BlockEntityRenderer<CropStandBlockEntity> {
    override fun render(stand: CropStandBlockEntity, partialTick: Float, pose: PoseStack, buffers: MultiBufferSource, light: Int, overlay: Int) {
        if (stand.isEmpty()) return
        context.blockRenderDispatcher.renderSingleBlock(stand.cropState, pose, buffers, light, overlay, ModelData.EMPTY, RenderType.cutout())
    }
}

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object CropStandClientEvents {
    @JvmStatic
    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        event.registerBlockEntityRenderer(ModBlockEntities.CROP_STAND.get(), ::CropStandRenderer)
    }
}

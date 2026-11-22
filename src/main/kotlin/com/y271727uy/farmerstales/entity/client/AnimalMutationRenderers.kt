package com.y271727uy.farmerstales.entity.client

import com.mojang.blaze3d.vertex.PoseStack
import com.y271727uy.farmerstales.FTMod
import com.y271727uy.farmerstales.client.model.ModernCowModel
import com.y271727uy.farmerstales.client.model.ModernPigModel
import com.y271727uy.farmerstales.entity.AnimalMutationEntities
import com.y271727uy.farmerstales.gameplay.animal_variants.client.AnimalVariantModelLayers
import net.minecraft.client.model.ChickenModel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.ChickenRenderer
import net.minecraft.client.renderer.entity.CowRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.PigRenderer
import net.minecraft.client.renderer.entity.RabbitRenderer
import net.minecraft.client.renderer.entity.SheepRenderer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.animal.Chicken
import net.minecraft.world.entity.animal.Cow
import net.minecraft.world.entity.animal.Pig
import net.minecraft.world.entity.animal.Rabbit
import net.minecraft.world.entity.animal.Sheep
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.EntityRenderersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object AnimalMutationRenderers {
    @JvmStatic
    @SubscribeEvent
    fun registerRenderers(event: EntityRenderersEvent.RegisterRenderers) {
        for (entry in AnimalMutationEntities.rabbits.types) {
            val skin = appearance(entry.get())
            event.registerEntityRenderer(entry.get()) { context ->
                object : RabbitRenderer(context) {
                    override fun getTextureLocation(rabbit: Rabbit) = skin.body
                }
            }
        }
        for (entry in AnimalMutationEntities.chickens.types) {
            val skin = appearance(entry.get())
            event.registerEntityRenderer(entry.get()) { context -> MutationChickenRenderer(context, skin) }
        }
        for (entry in AnimalMutationEntities.cows.types) {
            val skin = appearance(entry.get())
            event.registerEntityRenderer(entry.get()) { context ->
                object : CowRenderer(context) {
                    init {
                        model = ModernCowModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_COW))
                    }
                    override fun getTextureLocation(cow: Cow) = skin.body
                }
            }
        }
        for (entry in AnimalMutationEntities.sheep.types) {
            val skin = appearance(entry.get())
            event.registerEntityRenderer(entry.get()) { context ->
                object : SheepRenderer(context) {
                    override fun getTextureLocation(sheep: Sheep) = if (sheep.isSheared) skin.sheared else skin.body
                }
            }
        }
        for (entry in AnimalMutationEntities.pigs.types) {
            val skin = appearance(entry.get())
            event.registerEntityRenderer(entry.get()) { context ->
                object : PigRenderer(context) {
                    init {
                        model = ModernPigModel(context.bakeLayer(AnimalVariantModelLayers.MODERN_PIG))
                    }
                    override fun getTextureLocation(pig: Pig) = skin.body
                }
            }
        }
    }

    private fun appearance(type: EntityType<*>) = checkNotNull(AnimalMutationEntities.appearance(type))
}

private class MutationChickenRenderer(
    context: EntityRendererProvider.Context,
    private val appearance: AnimalMutationEntities.Appearance,
) : ChickenRenderer(context) {
    init {
        appearance.emissive?.let { texture ->
            val glow = RenderType.eyes(texture)
            addLayer(object : RenderLayer<Chicken, ChickenModel<Chicken>>(this) {
                override fun render(
                    pose: PoseStack, buffers: MultiBufferSource, light: Int, chicken: Chicken,
                    limbSwing: Float, limbSwingAmount: Float, partialTick: Float,
                    ageInTicks: Float, headYaw: Float, headPitch: Float,
                ) {
                    if (chicken.isInvisible) return
                    parentModel.renderToBuffer(
                        pose, buffers.getBuffer(glow), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        1f, 1f, 1f, 1f,
                    )
                }
            })
        }
    }

    override fun getTextureLocation(chicken: Chicken) = appearance.body
}

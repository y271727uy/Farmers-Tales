package com.y271727uy.farmerstales.gameplay.environment;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.GrassColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NaturalEnvironmentClient {
    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(SpringNatureEffects.FIREFLY.get(), FireflyParticle.Provider::new);
    }
    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(NaturalEnvironmentBlocks.SHORT_DRY_GRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(NaturalEnvironmentBlocks.TALL_DRY_GRASS.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(NaturalEnvironmentBlocks.BUSH.get(), RenderType.cutout());
        });
    }

    @SubscribeEvent
    public static void blockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(),
                NaturalEnvironmentBlocks.BUSH.get());
        event.register((state, level, pos, tint) -> tint == 1
                ? level != null && pos != null ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor()
                : -1, NaturalEnvironmentBlocks.WILDFLOWERS.get());
    }

    @SubscribeEvent
    public static void itemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> GrassColor.getDefaultColor(), NaturalEnvironmentBlocks.BUSH.get());
    }
}

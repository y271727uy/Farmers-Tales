package com.y271727uy.farmerstales.gameplay.environment.forest;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForestClientEvents {
    @SubscribeEvent
    public static void resolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(DryFoliageColors.RESOLVER);
    }

    @SubscribeEvent
    public static void reload(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DryFoliageColors());
    }

    @SubscribeEvent
    public static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> {
            return level == null || pos == null ? DryFoliageColors.DEFAULT : level.getBlockTint(pos, DryFoliageColors.RESOLVER);
        }, ForestBlocks.LEAF_LITTER.get());
    }

    @SubscribeEvent
    public static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> DryFoliageColors.DEFAULT, ForestBlocks.LEAF_LITTER.get());
    }
}

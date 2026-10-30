package com.y271727uy.farmerstales.gameplay.quality_crop;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.y271727uy.farmerstales.FTMod;

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LavaRoeEntityProtection {
    private static final TagKey<Item> FARMERSTALES_LAVA_ROE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("list", "lava_roe")
    );

    private LavaRoeEntityProtection() {
    }

    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) {
            return;
        }
        if (itemEntity.getItem().is(FARMERSTALES_LAVA_ROE)) {
            itemEntity.setInvulnerable(true);
        }
    }
}

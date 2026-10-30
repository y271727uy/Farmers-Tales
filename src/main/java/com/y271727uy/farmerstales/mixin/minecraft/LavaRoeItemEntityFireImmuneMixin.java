package com.y271727uy.farmerstales.mixin.minecraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ItemEntity.class, priority = 2000)
public abstract class LavaRoeItemEntityFireImmuneMixin {
    private static final TagKey<Item> FARMERSTALES_LAVA_ROE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("list", "lava_roe")
    );

    /**
     * ItemEntity uses this method before applying lava/fire damage. Keep the
     * vanilla item-level protection and add the runtime item tag directly.
     */
    @Overwrite
    public boolean fireImmune() {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        return itemEntity.getItem().is(FARMERSTALES_LAVA_ROE)
                || itemEntity.getItem().getItem().isFireResistant();
    }
}

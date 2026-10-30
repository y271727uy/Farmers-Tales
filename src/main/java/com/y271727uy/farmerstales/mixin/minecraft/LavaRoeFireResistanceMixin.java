package com.y271727uy.farmerstales.mixin.minecraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class LavaRoeFireResistanceMixin {
    private static final TagKey<Item> FARMERSTALES_LAVA_ROE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("list", "lava_roe")
    );

    @Inject(method = "isFireResistant()Z", at = @At("HEAD"), cancellable = true)
    private void farmerstales$makeLavaRoeFireResistant(CallbackInfoReturnable<Boolean> cir) {
        Item item = (Item) (Object) this;
        boolean isLavaRoe = BuiltInRegistries.ITEM.getTag(FARMERSTALES_LAVA_ROE)
                .map(items -> items.stream().anyMatch(holder -> holder.value() == item))
                .orElse(false);
        if (isLavaRoe) {
            cir.setReturnValue(true);
        }
    }
}

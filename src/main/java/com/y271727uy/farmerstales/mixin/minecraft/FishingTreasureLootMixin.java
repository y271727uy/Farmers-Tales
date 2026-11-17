package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.fish_group.FishingHookEnchantments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LootTableReference.class)
abstract class FishingTreasureLootMixin {
    @Shadow @Final private ResourceLocation name;

    @Inject(method = "createItemStack", at = @At("HEAD"), cancellable = true)
    private void farmerstales$replaceSomeTreasure(Consumer<ItemStack> output, LootContext context, CallbackInfo ci) {
        if (BuiltInLootTables.FISHING_TREASURE.equals(name)
                && context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHookEnchantments enchantments
                && enchantments.farmerstales$hasFishingFrenzy()
                && context.getRandom().nextFloat() < 0.10F) {
            context.getResolver().getLootTable(BuiltInLootTables.FISHING_FISH).getRandomItemsRaw(context, output);
            ci.cancel();
        }
    }
}

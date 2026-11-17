package com.y271727uy.farmerstales.gameplay.weather.area_weather;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.y271727uy.farmerstales.gameplay.quality_crop.util.BerryFruitHarvestWhitelist;
import com.y271727uy.farmerstales.gameplay.quality_crop.util.MDWhiteList;
import com.y271727uy.farmerstales.gameplay.quality_crop.util.NonStandardCropWhitelist;
import com.y271727uy.farmerstales.gameplay.quality_crop.util.StandardCropWhitelist;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import java.util.function.Supplier;

/** One final loot pass covers manual, scythe, crop-stand and automated block-loot harvests. */
public final class AreaWeatherHarvestModifier extends LootModifier {
    public static final Supplier<Codec<AreaWeatherHarvestModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, AreaWeatherHarvestModifier::new)));

    private AreaWeatherHarvestModifier(LootItemCondition[] conditions) { super(conditions); }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null) return loot;
        if (AreaWeatherEvents.current(context.getLevel()) != AreaWeather.NUCLEAR_RADIATION) return loot;
        var block = state.getBlock();
        // Immature crop destruction and seeds outside the produce whitelists are unaffected.
        if (block instanceof CropBlock crop && !crop.isMaxAge(state)) return loot;
        for (ItemStack stack : loot) {
            if ((block instanceof CropBlock && StandardCropWhitelist.INSTANCE.matches(stack))
                    || NonStandardCropWhitelist.INSTANCE.matches(stack, block)
                    || BerryFruitHarvestWhitelist.INSTANCE.matches(stack, block)
                    || MDWhiteList.INSTANCE.matches(stack, block)) {
                AreaWeatherEffects.reduceHarvest(context.getLevel(), stack);
            }
        }
        loot.removeIf(ItemStack::isEmpty);
        return loot;
    }

    @Override public Codec<? extends IGlobalLootModifier> codec() { return CODEC.get(); }
}

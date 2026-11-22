package com.y271727uy.farmerstales.gameplay.special_event.seasonal;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

import java.util.function.Supplier;

/** Applies seasonal bonuses once to the final block or fishing loot. */
public final class SeasonalEventLootModifier extends LootModifier {
    public static final Supplier<Codec<SeasonalEventLootModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(instance -> codecStart(instance).apply(instance, SeasonalEventLootModifier::new)));

    private SeasonalEventLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (loot.isEmpty()) return loot;
        var level = context.getLevel();
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null) {
            if (BuiltInLootTables.FISHING.equals(context.getQueriedLootTableId())
                    && SeasonalEvents.isActive(level, SeasonalEvent.FISH_TIDE)) {
                duplicate(loot);
            }
            return loot;
        }

        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)
                || !SeasonalEvents.isActive(level, SeasonalEvent.HARVEST_DAY)) {
            return loot;
        }
        if (context.getRandom().nextFloat() < 0.4F) duplicate(loot);

        var origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (origin != null && context.getRandom().nextFloat() < 0.6F) {
            var seed = crop.getCloneItemStack(level, BlockPos.containing(origin), state);
            if (!seed.isEmpty()) loot.add(seed.copyWithCount(2));
        }
        return loot;
    }

    private static void duplicate(ObjectArrayList<ItemStack> loot) {
        int originalSize = loot.size();
        for (int i = 0; i < originalSize; i++) {
            ItemStack stack = loot.get(i);
            if (!stack.isEmpty()) loot.add(stack.copy());
        }
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}

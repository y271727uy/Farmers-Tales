package com.y271727uy.farmerstales.all;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.gameplay.weather.area_weather.AreaWeatherHarvestModifier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Optional;
import java.util.function.Supplier;

/** Serializer for data-pack loot injections. */
public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FTMod.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ADD_ITEM =
            LOOT_MODIFIERS.register("add_item", () -> AddItemModifier.CODEC.get());

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> AREA_WEATHER_HARVEST =
            LOOT_MODIFIERS.register("area_weather_harvest", () -> AreaWeatherHarvestModifier.CODEC.get());

    private ModLootModifiers() {
    }

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }

    public static final class AddItemModifier extends LootModifier {
        private static final Supplier<Codec<AddItemModifier>> CODEC = Suppliers.memoize(() -> RecordCodecBuilder.create(instance ->
                codecStart(instance)
                        .and(instance.group(
                                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(modifier -> modifier.item),
                                CompoundTag.CODEC.optionalFieldOf("nbt").forGetter(modifier -> modifier.nbt)))
                        .apply(instance, AddItemModifier::new)));

        private final Item item;
        private final Optional<CompoundTag> nbt;

        private AddItemModifier(LootItemCondition[] conditions, Item item, Optional<CompoundTag> nbt) {
            super(conditions);
            this.item = item;
            this.nbt = nbt;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
            ItemStack stack = new ItemStack(item);
            nbt.ifPresent(tag -> stack.setTag(tag.copy()));
            generatedLoot.add(stack);
            return generatedLoot;
        }

        @Override
        public Codec<? extends IGlobalLootModifier> codec() {
            return CODEC.get();
        }
    }
}

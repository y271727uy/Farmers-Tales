package com.y271727uy.farmerstales.gameplay.animal_variants;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.registrate.ModRegistrate;
import com.y271727uy.farmerstales.variant.AnimalAppearanceVariantHolder;
import com.tterrag.registrate.providers.ProviderType;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class VariantEggs {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, FTMod.MODID);
    public static final RegistryObject<Item> BLUE_EGG = ITEMS.register("blue_egg", () -> new EggItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> BROWN_EGG = ITEMS.register("brown_egg", () -> new EggItem(new Item.Properties().stacksTo(16)));

    private VariantEggs() {}

    public static ItemLike laidBy(Chicken chicken, ItemLike original) {
        if (original != Items.EGG) return original;
        String variant = ((AnimalAppearanceVariantHolder) chicken).farmerstales$getAppearanceVariant();
        return variant.equals("cold") ? BLUE_EGG.get() : variant.equals("warm") ? BROWN_EGG.get() : original;
    }

    public static void setHatchlingVariant(ThrownEgg egg, Entity hatchling) {
        if (hatchling instanceof Chicken chicken) {
            String variant = egg.getItem().is(BLUE_EGG.get()) ? "cold" : egg.getItem().is(BROWN_EGG.get()) ? "warm" : "temperate";
            ((AnimalAppearanceVariantHolder) chicken).farmerstales$setAppearanceVariant(variant);
        }
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(VariantEggs::setup);
        ModRegistrate.REGISTRATE.addDataGenerator(ProviderType.LANG, provider -> {
            provider.add("item.farmerstales.blue_egg", "Blue Egg");
            provider.add("item.farmerstales.brown_egg", "Brown Egg");
        });
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var behavior = new AbstractProjectileDispenseBehavior() {
                @Override protected Projectile getProjectile(Level level, Position position, ItemStack stack) {
                    var egg = new ThrownEgg(level, position.x(), position.y(), position.z());
                    egg.setItem(stack);
                    return egg;
                }
            };
            DispenserBlock.registerBehavior(BLUE_EGG.get(), behavior);
            DispenserBlock.registerBehavior(BROWN_EGG.get(), behavior);
        });
    }
}

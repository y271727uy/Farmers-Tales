package com.y271727uy.farmerstales.client;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public final class AnimalAppearanceVariantTextures {
    private AnimalAppearanceVariantTextures() {
    }

    public static ResourceLocation farmAnimal(String animal, String variant) {
        return ResourceLocation.fromNamespaceAndPath(
                FTMod.MODID,
                "textures/entity/" + animal + "/" + variant + "_" + animal + ".png"
        );
    }

    public static ResourceLocation wolf(Wolf wolf, String variant) {
        if (variant.startsWith("mobvariants_")) {
            String breed = variant.substring("mobvariants_".length());
            String stateSuffix = wolf.isTame() ? "tame" : wolf.isAngry() ? "angry" : "wild";
            return ResourceLocation.fromNamespaceAndPath(
                    FTMod.MODID,
                    "textures/entity/wolf/" + breed + "_" + stateSuffix + ".png"
            );
        }
        String stateSuffix = wolf.isTame() ? "_tame" : wolf.isAngry() ? "_angry" : "";
        if ("pale".equals(variant)) {
            return ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf" + stateSuffix + ".png");
        }
        return ResourceLocation.fromNamespaceAndPath(
                FTMod.MODID,
                "textures/entity/wolf/wolf_" + variant + stateSuffix + ".png"
        );
    }
}

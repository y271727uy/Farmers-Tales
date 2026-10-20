package com.y271727uy.farmerstales.variant;

/** Stores the cosmetic variant selected when a supported vanilla animal is created. */
public interface AnimalAppearanceVariantHolder {
    String farmerstales$getAppearanceVariant();

    boolean farmerstales$hasAppearanceVariant();

    void farmerstales$setAppearanceVariant(String variant);
}

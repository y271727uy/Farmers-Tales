package com.y271727uy.farmerstales.gameplay.quality_crop

import com.y271727uy.farmerstales.mixin.minecraft.ItemEntityPickupDelayMixin
import net.minecraft.world.entity.item.ItemEntity

fun ItemEntity.harvestPickupDelay(): Int = (this as? ItemEntityPickupDelayMixin)?.pickupDelay ?: 0

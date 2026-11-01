package com.y271727uy.farmerstales.gameplay.fauna;
import net.minecraft.world.item.*;
public final class WolfArmorItem extends Item implements DyeableLeatherItem {
    public WolfArmorItem(Properties properties){super(properties);}
    @Override public boolean isValidRepairItem(ItemStack item,ItemStack material){return material.is(FaunaContent.SCUTE.get());}
}

package com.y271727uy.farmerstales.gameplay.environment.forest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
public final class TimberBoat extends Boat implements ForestBoatWood {
    private final String wood;
    public TimberBoat(EntityType<? extends Boat> type,Level level,String wood){super(type,level);this.wood=wood;}
    @Override public String wood(){return wood;}
    @Override public Item getDropItem(){return ForestTravel.ITEMS_BY_NAME.get(wood+"_boat").get();}
}

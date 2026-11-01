package com.y271727uy.farmerstales.gameplay.fauna;
import com.y271727uy.farmerstales.FTMod;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid=FTMod.MODID)
public final class ArmadilloInteractions {
    public static final class Flee extends AvoidEntityGoal<ArmadilloEntity>{public Flee(Spider spider){super(spider,ArmadilloEntity.class,6,1,1.2);}}
    @SubscribeEvent public static void join(EntityJoinLevelEvent event){if(!event.getLevel().isClientSide&&event.getEntity() instanceof Spider spider&&spider.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof Flee))spider.goalSelector.addGoal(1,new Flee(spider));}
}

package com.y271727uy.farmerstales.gameplay.creaking;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Attach to both newly spawned and existing illagers when their chunks load. */
@Mod.EventBusSubscriber(modid = FTMod.MODID)
public final class CreakingInteractions {
    private CreakingInteractions() {}
    public static final class FleeCreakingGoal extends AvoidEntityGoal<CreakingEntity> {
        public FleeCreakingGoal(AbstractIllager illager) { super(illager, CreakingEntity.class, 8, 1, 1.2); }
    }
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof AbstractIllager illager
                && illager.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof FleeCreakingGoal)) {
            illager.goalSelector.addGoal(0, new FleeCreakingGoal(illager));
        }
    }
}

package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.CrimsonWolfBreakFenceGoal;
import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.LivestockRaiderState;
import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.RaidWolfFleeDeterrentGoal;
import com.y271727uy.farmerstales.gameplay.animal_husbandry.raid.RaidWolfPlayerTargetGoal;
import com.y271727uy.farmerstales.gameplay.animal_husbandry.shepherd.ShepherdDogGoal;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Installs shepherding with the wolf's native goals, rather than waiting for an entity join event. */
@Mixin(Wolf.class)
public abstract class WolfShepherdingMixin {
    @Inject(method = "registerGoals", at = @At("RETURN"))
    private void farmerstales$addShepherdingGoal(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        wolf.goalSelector.addGoal(0, new ShepherdDogGoal(wolf));
        wolf.goalSelector.addGoal(1, new RaidWolfFleeDeterrentGoal(wolf));
        wolf.goalSelector.addGoal(2, new CrimsonWolfBreakFenceGoal(wolf));
        wolf.targetSelector.addGoal(4, new RaidWolfPlayerTargetGoal(wolf));
    }

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void farmerstales$keepRaidAnger(CallbackInfo ci) {
        Wolf wolf = (Wolf) (Object) this;
        if (!wolf.level().isClientSide && LivestockRaiderState.isRaider(wolf) && !wolf.isTame()) {
            wolf.setRemainingPersistentAngerTime(24_000);
        }
    }
}

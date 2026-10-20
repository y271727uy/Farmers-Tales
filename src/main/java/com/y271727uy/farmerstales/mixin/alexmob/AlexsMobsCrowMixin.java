package com.y271727uy.farmerstales.mixin.alexmob;

import com.y271727uy.farmerstales.gameplay.crow.CrowFarmScareGoal;
import com.y271727uy.farmerstales.gameplay.crow.CrowScarecrowAvoidanceGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a farmer-aware flight response without depending on Alex's Mobs at runtime. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCrow")
public abstract class AlexsMobsCrowMixin {
    @Inject(method = {"registerGoals", "m_8099_"}, at = @At("TAIL"), require = 0)
    private void farmerstales$addFarmScareGoal(CallbackInfo ci) {
        Mob crow = (Mob) (Object) this;
        crow.goalSelector.addGoal(1, new CrowFarmScareGoal(crow));
        crow.goalSelector.addGoal(1, new CrowScarecrowAvoidanceGoal(crow));
    }
}

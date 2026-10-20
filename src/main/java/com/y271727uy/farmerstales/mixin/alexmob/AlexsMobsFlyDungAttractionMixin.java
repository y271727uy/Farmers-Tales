package com.y271727uy.farmerstales.mixin.alexmob;

import com.y271727uy.farmerstales.gameplay.animal_husbandry.goal.DungAttractionGoal;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional Alex's Mobs support: flies move toward nearby dung piles. */
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityFly")
public abstract class AlexsMobsFlyDungAttractionMixin {
    @Inject(method = {"registerGoals", "m_8099_"}, at = @At("TAIL"), require = 0)
    private void farmerstales$addDungAttractionGoal(CallbackInfo ci) {
        Mob fly = (Mob) (Object) this;
        fly.goalSelector.addGoal(2, new DungAttractionGoal(fly));
    }
}

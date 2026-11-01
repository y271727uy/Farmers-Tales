package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.all.NaturalEnvironmentBlocks;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Predicate;

/** Keep vanilla eating animation, wool regrowth and Forge mobGriefing handling. */
@Mixin(EatBlockGoal.class)
public abstract class EatDryGrassMixin {
    @Redirect(method = {"canUse", "tick"}, at = @At(value = "INVOKE",
            target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    private boolean farmerstales$includeDryGrass(Predicate<BlockState> predicate, Object value) {
        return predicate.test((BlockState) value) || NaturalEnvironmentBlocks.isDryGrass((BlockState) value);
    }
}

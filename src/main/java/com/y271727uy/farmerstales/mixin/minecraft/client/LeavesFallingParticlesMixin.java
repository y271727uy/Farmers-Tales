package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.gameplay.environment.forest.client.ForestLeafParticleClient;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesFallingParticlesMixin {
    @Inject(method = "animateTick", at = @At("TAIL"))
    private void farmerstales$fallingLeaves(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        ForestLeafParticleClient.animate(state, level, pos, random);
    }
}

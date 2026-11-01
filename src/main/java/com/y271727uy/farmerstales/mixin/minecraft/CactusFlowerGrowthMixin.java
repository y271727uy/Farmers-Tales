package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.environment.SpringPlants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public abstract class CactusFlowerGrowthMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void farmerstales$growFlower(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (SpringPlants.tickCactusFlower(state, level, pos, random)) callback.cancel();
    }
}

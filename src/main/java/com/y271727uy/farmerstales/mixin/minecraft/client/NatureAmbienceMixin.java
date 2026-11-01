package com.y271727uy.farmerstales.mixin.minecraft.client;
import com.y271727uy.farmerstales.gameplay.environment.forest.NatureAmbience;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class NatureAmbienceMixin {
    @Inject(method="animateTick",at=@At("HEAD")) private void naturalSounds(BlockState state,Level level,BlockPos pos,RandomSource random,CallbackInfo ci){NatureAmbience.animate(state,level,pos,random);}
}

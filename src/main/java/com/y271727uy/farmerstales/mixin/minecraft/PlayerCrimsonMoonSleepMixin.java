package com.y271727uy.farmerstales.mixin.minecraft;

import com.y271727uy.farmerstales.gameplay.special_event.crimson_moon.CrimsonMoonSleep;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Slows the vanilla sleep timer so falling asleep takes longer without breaking the overlay formula. */
@Mixin(Player.class)
public abstract class PlayerCrimsonMoonSleepMixin {
    @Shadow
    private int sleepCounter;

    @Inject(method = "tick", at = @At("HEAD"))
    private void farmerstales$stretchCrimsonSleep(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (this.sleepCounter <= 0 || !CrimsonMoonSleep.shouldSkipSleepTick(player)) {
            return;
        }
        this.sleepCounter--;
    }
}

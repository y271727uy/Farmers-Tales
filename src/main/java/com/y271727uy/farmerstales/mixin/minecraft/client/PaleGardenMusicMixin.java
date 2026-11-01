package com.y271727uy.farmerstales.mixin.minecraft.client;

import com.y271727uy.farmerstales.gameplay.environment.forest.ForestBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Pale Garden has no background music, including vanilla's fallback game music. */
@Mixin(MusicManager.class)
public abstract class PaleGardenMusicMixin {
    @Shadow public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void farmerstales$paleGardenSilence(CallbackInfo callback) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null
                && minecraft.level.getBiome(minecraft.player.blockPosition()).is(ForestBiomes.PALE_GARDEN)) {
            stopPlaying();
            callback.cancel();
        }
    }
}

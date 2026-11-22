package com.y271727uy.farmerstales.mixin.untitledduckmod;

import com.y271727uy.farmerstales.integration.untitledduckmod.UntitledDuckIntegration;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Enroll waterfowl tamed through their original interaction in husbandry. */
@Mixin(TamableAnimal.class)
public abstract class WaterfowlTamingMixin {
    @Inject(method = "tame", at = @At("RETURN"))
    private void farmerstales$trackWaterfowlTaming(Player player, CallbackInfo ci) {
        UntitledDuckIntegration.onNativeTame((TamableAnimal) (Object) this);
    }
}

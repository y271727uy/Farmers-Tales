package com.y271727uy.farmerstales.mixin.xaero;

import com.y271727uy.farmerstales.integration.xaero.XaeroClockIntegration;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.hud.minimap.info.BuiltInInfoDisplays;
import xaero.hud.minimap.info.InfoDisplay;

@Mixin(value = BuiltInInfoDisplays.class, remap = false)
public abstract class BuiltInInfoDisplaysMixin {
    @Shadow
    private static List<InfoDisplay<?>> ALL;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void farmerstales$registerClock(CallbackInfo ci) {
        XaeroClockIntegration.register(ALL);
    }
}

package com.y271727uy.farmerstales.mixin.xaero;

import com.y271727uy.farmerstales.integration.xaero.XaeroClockIntegration;
import java.util.Iterator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xaero.hud.minimap.Minimap;
import xaero.hud.minimap.info.InfoDisplay;
import xaero.hud.minimap.info.render.InfoDisplayRenderer;
import xaero.hud.minimap.module.MinimapSession;

@Mixin(value = InfoDisplayRenderer.class, remap = false)
public abstract class InfoDisplayRendererMixin {
    @ModifyVariable(method = "render", at = @At("STORE"), ordinal = 0)
    private Iterator<InfoDisplay<?>> farmerstales$orderBelowSeason(
            Iterator<InfoDisplay<?>> displays, GuiGraphics graphics, MinimapSession session,
            Minimap minimap, int height, int size, BlockPos playerPos,
            int scaledX, int scaledY, float mapScale, MultiBufferSource.BufferSource buffers) {
        boolean belowMap = scaledY + size / 2 < (int) (height * mapScale) / 2;
        return XaeroClockIntegration.orderForRendering(displays, belowMap);
    }
}

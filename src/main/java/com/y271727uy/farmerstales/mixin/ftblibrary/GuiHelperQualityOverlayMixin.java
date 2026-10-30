package com.y271727uy.farmerstales.mixin.ftblibrary;

import com.y271727uy.farmerstales.gameplay.quality_crop.client.QualityOverlayRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FTB draws items and decorations itself, bypassing GuiGraphics.renderItemDecorations. */
@Pseudo
@Mixin(targets = "dev.ftb.mods.ftblibrary.ui.GuiHelper", remap = false)
public abstract class GuiHelperQualityOverlayMixin {
    @Inject(method = "drawItem(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;IZLjava/lang/String;)V", at = @At("TAIL"), remap = false, require = 0)
    private static void farmerstales$renderQualityOverlay(GuiGraphics graphics, ItemStack stack, int seed, boolean drawOverlay, String countText, CallbackInfo ci) {
        if (!stack.isEmpty()) {
            // FTB's current pose is centered on the icon, rather than its top-left corner.
            // FTB scales Z along with the icon. Use its decoration depth, not the
            // inventory's +200 offset, so enlarged stars stay behind tooltips.
            QualityOverlayRenderer.render(graphics, stack, -8, -8, 20.0);
        }
    }
}

package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import forestry.apiimpl.client.genetics.AnalyzerScreenGraphics;
import forestry.core.gui.PortableAnalyzerScreen;
import forestry.core.inventory.ItemInventoryAlyzer;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The portable analyzer draws its active and inactive columns only for a Forestry individual.
 * A gene seed has no individual, so the same page is drawn from its own genome once it is identified.
 */
@Pseudo
@Mixin(targets = "forestry.core.gui.PortableAnalyzerScreen", remap = false)
public abstract class PortableAnalyzerScreenMixin {
    private static final int COLUMN_LABEL = 8;
    private static final int ROW_START = 24;
    private static final int ROW_HEIGHT = 12;

    @Shadow
    @Final
    private ItemInventoryAlyzer itemInventory;

    /**
     * The help overview is what overlaps the gene rows. Once a seed is identified,
     * that overview is skipped and the gene columns are the page.
     */
    @Inject(method = "drawAnalyticsOverview", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void farmerstales$hideOverview(GuiGraphics graphics, CallbackInfo callback) {
        if (itemInventory != null && geneSlot() >= 0) {
            callback.cancel();
        }
    }

    @Inject(method = {"renderBg", "m_7286_"}, at = @At("RETURN"), remap = false, require = 0)
    private void farmerstales$drawGenePage(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo callback) {
        if (itemInventory == null) {
            return;
        }
        int slot = geneSlot();
        if (slot < 0) {
            return;
        }
        var stack = itemInventory.getItem(slot);
        List<GeneCropGenome.Trait> traits = GeneCropGenome.traits(stack);
        if (traits.isEmpty()) {
            return;
        }

        var screen = (AbstractContainerScreen<?>) (Object) this;
        Font font = screen.getMinecraft().font;
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        draw(graphics, font, left, top, Component.translatable("for.gui.active"), AnalyzerScreenGraphics.COLUMN_1, 8, 0xFFFFFF);
        draw(graphics, font, left, top, Component.translatable("for.gui.inactive"), AnalyzerScreenGraphics.COLUMN_2, 8, 0xFFFFFF);
        icon(graphics, stack, left, top, AnalyzerScreenGraphics.COLUMN_1);
        icon(graphics, stack, left, top, AnalyzerScreenGraphics.COLUMN_2);
        int row = 0;
        for (GeneCropGenome.Trait trait : traits) {
            int y = ROW_START + row * ROW_HEIGHT;
            draw(graphics, font, left, top, trait.getLabel(), COLUMN_LABEL, y, 0xFFFFFF);
            draw(graphics, font, left, top, trait.getActive(), AnalyzerScreenGraphics.COLUMN_1, y, PortableAnalyzerScreen.getColorCoding(true));
            draw(graphics, font, left, top, trait.getInactive(), AnalyzerScreenGraphics.COLUMN_2, y, PortableAnalyzerScreen.getColorCoding(false));
            row++;
        }
    }

    /**
     * Forestry's specimen search only accepts an individual. A gene seed is not one,
     * so the page has to find the first identified seed in the result slots itself.
     */
    private int geneSlot() {
        for (int slot = 2; slot <= 6; slot++) {
            if (GeneCropGenome.isIdentified(itemInventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    /** A crop has no Forestry species icon, so the specimen itself stands in for both columns. */
    private void icon(GuiGraphics graphics, ItemStack specimen, int left, int top, int column) {
        graphics.renderItem(specimen.copyWithCount(1), left + column + 28, top + 4);
    }

    private void draw(GuiGraphics graphics, Font font, int left, int top, Component text, int x, int y, int color) {
        graphics.drawString(font, text, left + x, top + y, color, false);
    }
}

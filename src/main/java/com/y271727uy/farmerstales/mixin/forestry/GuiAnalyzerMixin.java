package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import forestry.apiimpl.client.genetics.AnalyzerScreenGraphics;
import forestry.core.gui.PortableAnalyzerScreen;
import forestry.core.tiles.TileAnalyzer;
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
 * The block analyzer draws its gene page on the pedestal, and only for a Forestry individual.
 * An identified gene seed has no individual, so the same columns are drawn here.
 */
@Pseudo
@Mixin(targets = "forestry.core.gui.GuiAnalyzer", remap = false)
public abstract class GuiAnalyzerMixin {
    private static final int COLUMN_LABEL = 8;
    private static final int ROW_START = 24;
    private static final int ROW_HEIGHT = 12;

    @Shadow
    @Final
    private TileAnalyzer tile;

    @Inject(method = {"renderBg", "m_7286_"}, at = @At("RETURN"), remap = false, require = 0)
    private void farmerstales$drawGenePage(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo callback) {
        ItemStack seed = tile.getIndividualOnDisplay();
        List<GeneCropGenome.Trait> traits = GeneCropGenome.traits(seed);
        if (traits.isEmpty()) {
            return;
        }
        var screen = (AbstractContainerScreen<?>) (Object) this;
        Font font = screen.getMinecraft().font;
        int left = screen.getGuiLeft();
        int top = screen.getGuiTop();
        draw(graphics, font, left, top, Component.translatable("for.gui.active"), AnalyzerScreenGraphics.COLUMN_1, 8, 0xFFFFFF);
        draw(graphics, font, left, top, Component.translatable("for.gui.inactive"), AnalyzerScreenGraphics.COLUMN_2, 8, 0xFFFFFF);
        icon(graphics, seed, left, top, AnalyzerScreenGraphics.COLUMN_1);
        icon(graphics, seed, left, top, AnalyzerScreenGraphics.COLUMN_2);
        int row = 0;
        for (GeneCropGenome.Trait trait : traits) {
            int y = ROW_START + row * ROW_HEIGHT;
            draw(graphics, font, left, top, trait.getLabel(), COLUMN_LABEL, y, 0xFFFFFF);
            draw(graphics, font, left, top, trait.getActive(), AnalyzerScreenGraphics.COLUMN_1, y, PortableAnalyzerScreen.getColorCoding(true));
            draw(graphics, font, left, top, trait.getInactive(), AnalyzerScreenGraphics.COLUMN_2, y, PortableAnalyzerScreen.getColorCoding(false));
            row++;
        }
    }

    private void icon(GuiGraphics graphics, ItemStack specimen, int left, int top, int column) {
        graphics.renderItem(specimen.copyWithCount(1), left + column + 28, top + 4);
    }

    private void draw(GuiGraphics graphics, Font font, int left, int top, Component text, int x, int y, int color) {
        graphics.drawString(font, text, left + x, top + y, color, false);
    }
}

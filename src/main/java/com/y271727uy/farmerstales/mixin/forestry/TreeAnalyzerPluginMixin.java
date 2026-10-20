package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.integration.forestry.PestResistanceDisplay;
import forestry.api.arboriculture.ITreeSpecies;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.client.genetics.IAnalyzerGraphics;
import forestry.api.genetics.ILifeStage;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The tree analyzer draws a fixed set of rows. Pest resistance follows the
 * rows on the first page, which is the page shown in the screenshot.
 */
@Pseudo
@Mixin(targets = "forestry.plugin.client.TreeAnalyzerPlugin")
public abstract class TreeAnalyzerPluginMixin {
    /**
     * Page 1 leaves one blank line between the species row and sapling yield.
     * Dropping that line lifts sapling yield and every row under it, so resistance stays on the page.
     * The species row is already drawn and does not move.
     */
    @Redirect(
            method = "drawPage1(Lforestry/api/client/genetics/IAnalyzerGraphics;Lforestry/api/arboriculture/genetics/ITree;Lforestry/api/genetics/ILifeStage;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lforestry/api/client/genetics/IAnalyzerGraphics;addLineSpacing(I)V"
            ),
            require = 0
    )
    private void farmerstales$closeSpeciesGap(IAnalyzerGraphics<ITreeSpecies, ITree> graphics, int lines) {
    }

    @Inject(
            method = "drawPage1(Lforestry/api/client/genetics/IAnalyzerGraphics;Lforestry/api/arboriculture/genetics/ITree;Lforestry/api/genetics/ILifeStage;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("RETURN"),
            require = 0
    )
    private void farmerstales$drawPestResistance(
            IAnalyzerGraphics<ITreeSpecies, ITree> graphics,
            ITree individual,
            ILifeStage stage,
            ItemStack stack,
            CallbackInfo callback
    ) {
        if (individual.getGenome().getKaryotype().contains(PestResistanceDisplay.chromosome())) {
            graphics.drawChromosomeRow(PestResistanceDisplay.chromosome());
        }
    }
}

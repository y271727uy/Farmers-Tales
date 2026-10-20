package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.integration.forestry.PestResistanceDisplay;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.genetics.IGenome;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tree sapling tooltips list chromosomes by hand. The pest chromosome is appended
 * after that list, and only once the sapling has been analyzed.
 */
@Pseudo
@Mixin(targets = "forestry.arboriculture.TreeSpecies")
public abstract class TreeSpeciesTooltipMixin {
    @Inject(
            method = "addTooltip(Lforestry/api/arboriculture/genetics/ITree;Ljava/util/List;)V",
            at = @At("RETURN"),
            require = 0
    )
    private void farmerstales$appendPestResistance(ITree tree, List<Component> tooltip, CallbackInfo callback) {
        if (!tree.isAnalyzed()) {
            return;
        }
        IGenome genome = tree.getGenome();
        if (!genome.getKaryotype().contains(PestResistanceDisplay.chromosome())) {
            return;
        }
        tooltip.add(Component.literal("R:")
                .append(genome.getActiveName(PestResistanceDisplay.chromosome()))
                .withStyle(ChatFormatting.DARK_GREEN));
    }
}

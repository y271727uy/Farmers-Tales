package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Analyzer input slots otherwise accept only Forestry individuals.
 * InventoryAnalyzer.SLOT_INPUT_1 is 2 and SLOT_INPUT_COUNT is 6, so slots 2 through 7.
 * A gene seed is accepted there and nowhere else, so the honey-can slot stays unchanged.
 */
@Pseudo
@Mixin(targets = "forestry.core.inventory.InventoryAnalyzer")
public abstract class InventoryAnalyzerMixin {
    private static final int INPUT_FIRST = 2;
    private static final int INPUT_COUNT = 6;

    @Inject(method = "canSlotAccept", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$acceptGeneSeed(int slotIndex, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (slotIndex >= INPUT_FIRST && slotIndex < INPUT_FIRST + INPUT_COUNT && GeneCropGenome.isGeneSeed(stack)) {
            callback.setReturnValue(true);
        }
    }
}

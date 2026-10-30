package com.y271727uy.farmerstales.gameplay.gene_crop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** The generation being popped by the current harvest. A plain drop has none. */
public final class GeneCropDrop {
    private static final ThreadLocal<CompoundTag> GENERATION = new ThreadLocal<>();

    private GeneCropDrop() {
    }

    public static void prepare(CompoundTag genome) {
        GENERATION.set(genome);
    }

    public static ItemStack apply(ItemStack stack) {
        CompoundTag genome = current();
        if (genome == null || stack.isEmpty() || !GeneCropGenome.isGeneSeed(stack) || GeneCropGenome.isIdentified(stack)) {
            return stack;
        }
        stack.getOrCreateTag().put(GeneCropGenome.TAG, genome.copy());
        return stack;
    }

    public static void clear() {
        GENERATION.remove();
    }

    @Nullable
    public static CompoundTag current() {
        return GENERATION.get();
    }
}

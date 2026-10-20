package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import java.lang.reflect.Field;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The portable analyzer only accepts Forestry individuals and spends one honey drop per analysis.
 * A gene seed uses the same specimen slot and the same fuel, then lands in the first result slot.
 * Slot access stays on this inventory instead of a @Shadow, which cannot resolve in the development environment.
 */
@Pseudo
@Mixin(targets = "forestry.core.inventory.ItemInventoryAlyzer")
public abstract class ItemInventoryAlyzerMixin {
    private static final int FUEL_SLOT = 0;
    private static final int SPECIMEN_SLOT = 1;
    private static final int RESULT_SLOT = 2;

    @Inject(method = "canSlotAccept", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$acceptGeneSeed(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (slot == FUEL_SLOT || !GeneCropGenome.isGeneSeed(stack)) {
            return;
        }
        if (slot == SPECIMEN_SLOT || stackAt(slot).isEmpty()) {
            callback.setReturnValue(true);
        }
    }

    /**
     * Forestry converts the specimen before analysis and drops anything that is not an individual.
     * The seed has to be identified at placement, before that conversion clears it.
     */
    @Inject(method = "m_6836_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void farmerstales$placeGeneSeed(int slot, ItemStack stack, CallbackInfo callback) {
        if (slot != SPECIMEN_SLOT || !GeneCropGenome.isGeneSeed(stack)) {
            return;
        }
        callback.cancel();
        put(SPECIMEN_SLOT, stack.copy());
        analyze(stackAt(SPECIMEN_SLOT));
    }

    @Inject(method = "analyzeSpecimen", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$analyzeGeneSeed(ItemStack specimen, CallbackInfo callback) {
        if (!GeneCropGenome.isGeneSeed(specimen)) {
            return;
        }
        callback.cancel();
        analyze(specimen);
    }

    private void analyze(ItemStack specimen) {
        if (!GeneCropGenome.isGeneSeed(specimen)) {
            return;
        }
        if (!GeneCropGenome.isIdentified(specimen)) {
            if (!forestry.core.inventory.ItemInventoryAlyzer.isAlyzingFuel(stackAt(FUEL_SLOT))) {
                return;
            }
            Player player = player();
            RandomSource random = player == null || player.level() == null
                    ? RandomSource.createNewThreadLocalInstance()
                    : player.level().getRandom();
            GeneCropGenome.identify(specimen, random);
            shrink(FUEL_SLOT);
        }
        put(RESULT_SLOT, specimen);
        put(SPECIMEN_SLOT, ItemStack.EMPTY);
    }

    private ItemStack stackAt(int slot) {
        NonNullList<ItemStack> stacks = stacks();
        return stacks == null || slot >= stacks.size() ? ItemStack.EMPTY : stacks.get(slot);
    }

    private void put(int slot, ItemStack stack) {
        NonNullList<ItemStack> stacks = stacks();
        if (stacks != null && slot < stacks.size()) {
            stacks.set(slot, stack);
            writeBack();
        }
    }

    private void shrink(int slot) {
        ItemStack stack = stackAt(slot);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            writeBack();
        }
    }

    @SuppressWarnings("unchecked")
    private NonNullList<ItemStack> stacks() {
        Class<?> type = getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("inventoryStacks");
                field.setAccessible(true);
                return (NonNullList<ItemStack>) field.get(this);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException failure) {
                throw new IllegalStateException(failure);
            }
        }
        return null;
    }

    private void writeBack() {
        Class<?> type = getClass();
        while (type != null && type != ItemInventoryAlyzerMixin.class) {
            for (java.lang.reflect.Method method : type.getDeclaredMethods()) {
                if (!method.getName().equals("writeToParentNBT") || method.getParameterCount() != 0) {
                    continue;
                }
                method.setAccessible(true);
                try {
                    method.invoke(this);
                } catch (ReflectiveOperationException failure) {
                    throw new IllegalStateException(failure);
                }
                return;
            }
            type = type.getSuperclass();
        }
    }

    private Player player() {
        Class<?> type = getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("player");
                field.setAccessible(true);
                Object value = field.get(this);
                return value instanceof Player player ? player : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException failure) {
                return null;
            }
        }
        return null;
    }
}

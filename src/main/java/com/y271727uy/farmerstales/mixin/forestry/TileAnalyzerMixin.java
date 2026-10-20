package com.y271727uy.farmerstales.mixin.forestry;

import com.y271727uy.farmerstales.gameplay.gene_crop.GeneCropGenome;
import forestry.core.tiles.TileAnalyzer;
import forestry.core.utils.InventoryUtil;
import java.lang.reflect.Field;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Forestry refuses to pull a stack into the analyze slot unless it has an individual.
 * Gene seeds are pulled after that search misses, then identified with the same 100 mB of honey.
 * An already identified seed only passes through. Bees, trees and butterflies keep the original path.
 * Members inherited from TilePowered are reached by descriptor because a @Shadow cannot be
 * resolved against this optional mod in the development environment.
 */
@Pseudo
@Mixin(targets = "forestry.core.tiles.TileAnalyzer")
public abstract class TileAnalyzerMixin {
    private static final int ANALYZE_SLOT = 0;

    @Inject(method = "getInputSlotIndex", at = @At("RETURN"), cancellable = true, require = 0)
    private void farmerstales$selectGeneSeed(CallbackInfoReturnable<Integer> callback) {
        if (callback.getReturnValue() != null) {
            return;
        }
        Container input = input();
        if (input == null) {
            return;
        }
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            if (GeneCropGenome.isGeneSeed(input.getItem(slot))) {
                callback.setReturnValue(slot);
                return;
            }
        }
    }

    @Inject(method = "moveSpecimenToAnalyzeSlot", at = @At("RETURN"), require = 0)
    private void farmerstales$stageGeneSeed(CallbackInfo callback) {
        if (!stackIn(ANALYZE_SLOT).isEmpty()) {
            return;
        }
        Container input = input();
        if (input == null) {
            return;
        }
        int slot = -1;
        for (int index = 0; index < input.getContainerSize(); index++) {
            if (GeneCropGenome.isGeneSeed(input.getItem(index))) {
                slot = index;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        ItemStack seed = input.getItem(slot);
        replace(ANALYZE_SLOT, seed);
        input.setItem(slot, ItemStack.EMPTY);
        sendDisplay(seed);
        if (GeneCropGenome.isIdentified(seed)) {
            setCycle(1, 0);
        } else {
            setCycle(GeneCropGenome.ticksPerWork(), TileAnalyzer.analyzerEnergyPerWork);
        }
    }

    @Inject(method = "workCycle", at = @At("HEAD"), cancellable = true, require = 0)
    private void farmerstales$identifyGeneSeed(CallbackInfoReturnable<Boolean> callback) {
        ItemStack seed = stackIn(ANALYZE_SLOT);
        if (!GeneCropGenome.isGeneSeed(seed)) {
            return;
        }
        if (!GeneCropGenome.isIdentified(seed) && !drainHoney()) {
            callback.setReturnValue(false);
            return;
        }
        if (!GeneCropGenome.isIdentified(seed)) {
            Level level = ((BlockEntity) (Object) this).getLevel();
            RandomSource random = level == null ? RandomSource.createNewThreadLocalInstance() : level.getRandom();
            GeneCropGenome.identify(seed, random);
        }
        Container output = (Container) field("invOutput");
        if (output == null || !InventoryUtil.tryAddStack(output, seed, true)) {
            callback.setReturnValue(false);
            return;
        }
        replace(ANALYZE_SLOT, ItemStack.EMPTY);
        sendDisplay(ItemStack.EMPTY);
        callback.setReturnValue(true);
    }

    /**
     * The pedestal renders the client-only display stack, filled by Forestry's display packet.
     * Gene seeds are not individuals, so the packet has to be sent when the seed enters and when it leaves.
     */
    private void sendDisplay(ItemStack stack) {
        BlockEntity tile = (BlockEntity) (Object) this;
        Level level = tile.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        try {
            Class<?> tileType = Class.forName("forestry.core.tiles.TileForestry");
            Class<?> packetType = Class.forName("forestry.core.network.packets.PacketItemStackDisplay");
            Object packet = packetType.getConstructor(tileType, ItemStack.class).newInstance(tile, stack);
            Class.forName("forestry.core.utils.NetworkUtil")
                    .getMethod("sendNetworkPacket", Class.forName("forestry.api.modules.IForestryPacketClient"), net.minecraft.core.BlockPos.class, Level.class)
                    .invoke(null, packet, tile.getBlockPos(), level);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private boolean drainHoney() {
        Object tank = field("resourceTank");
        if (!(tank instanceof IFluidHandler fluids)) {
            return false;
        }
        int amount = GeneCropGenome.honeyPerWork();
        FluidStack drained = fluids.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty() || drained.getAmount() != amount) {
            return false;
        }
        fluids.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private void setCycle(int ticks, int energy) {
        setInt("ticksPerWorkCycle", ticks);
        setInt("energyPerWorkCycle", energy);
    }

    private void setInt(String name, int value) {
        try {
            Field target = find(name);
            target.setAccessible(true);
            target.setInt(this, value);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(name, failure);
        }
    }

    private Field find(String name) throws NoSuchFieldException {
        Class<?> type = getClass();
        while (type != null) {
            try {
                return type.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private ItemStack stackIn(int slot) {
        Container inventory = inventory();
        return inventory == null ? ItemStack.EMPTY : inventory.getItem(slot);
    }

    private void replace(int slot, ItemStack stack) {
        Container inventory = inventory();
        if (inventory != null) {
            inventory.setItem(slot, stack);
        }
    }

    private Container inventory() {
        Object value = field("inventory");
        return value instanceof Container container ? container : null;
    }

    private Container input() {
        Object value = field("invInput");
        return value instanceof Container container ? container : null;
    }

    private Object field(String name) {
        try {
            Field target = find(name);
            target.setAccessible(true);
            return target.get(this);
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }
}

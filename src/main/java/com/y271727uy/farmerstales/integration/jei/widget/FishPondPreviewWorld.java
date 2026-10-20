package com.y271727uy.farmerstales.integration.jei.widget;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;

/**
 * ldlib 的 DummyWorld 把流体查询固定成空。这里改回方块自身的流体状态，水和熔岩才能画出来。
 */
public class FishPondPreviewWorld extends TrackedDummyWorld {
    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }
}

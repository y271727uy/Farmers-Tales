package com.y271727uy.farmerstales.gameplay.quality_crop;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Associates block drops with the player who broke or picked the crop. */
public final class QualityHarvestContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private QualityHarvestContext() {
    }

    public static void capture(Level level, Player player, BlockState state) {
        if (level.isClientSide()) {
            return;
        }
        CURRENT.set(new Context(level, player, state, false));
    }

    public static void captureScythe(Level level, Player player, BlockState state) {
        CURRENT.set(new Context(level, player, state, true));
    }

    public static Context current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record Context(Level level, Player player, BlockState state, boolean scythePick) {
    }
}

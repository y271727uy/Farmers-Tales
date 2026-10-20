package com.y271727uy.farmerstales.gameplay.animal_husbandry.goal;

import com.y271727uy.farmerstales.all.ModBlocks;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Moves a fly toward the closest dung pile without consuming or changing it. */
public final class DungAttractionGoal extends Goal {
    private static final int SEARCH_RADIUS = 5;
    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final double MOVE_SPEED = 0.8D;

    private final Mob fly;
    @Nullable
    private BlockPos target;
    private int searchCooldown;

    public DungAttractionGoal(Mob fly) {
        this.fly = fly;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.fly.level().isClientSide || this.searchCooldown-- > 0) {
            return false;
        }

        this.searchCooldown = SEARCH_INTERVAL_TICKS;
        this.target = this.findClosestDung();
        return this.target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.target != null
                && this.fly.level().getBlockState(this.target).is(ModBlocks.ANIMAL_DUNG.get())
                && this.fly.distanceToSqr(this.target.getX() + 0.5D, this.target.getY() + 0.5D, this.target.getZ() + 0.5D)
                <= (SEARCH_RADIUS + 1D) * (SEARCH_RADIUS + 1D);
    }

    @Override
    public void tick() {
        if (this.target == null) {
            return;
        }

        double x = this.target.getX() + 0.5D;
        double y = this.target.getY() + 0.75D;
        double z = this.target.getZ() + 0.5D;
        this.fly.getLookControl().setLookAt(x, y, z);
        this.fly.getMoveControl().setWantedPosition(x, y, z, MOVE_SPEED);
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Nullable
    private BlockPos findClosestDung() {
        BlockPos origin = this.fly.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (int xOffset = -SEARCH_RADIUS; xOffset <= SEARCH_RADIUS; xOffset++) {
            for (int yOffset = -SEARCH_RADIUS; yOffset <= SEARCH_RADIUS; yOffset++) {
                for (int zOffset = -SEARCH_RADIUS; zOffset <= SEARCH_RADIUS; zOffset++) {
                    double distance = xOffset * xOffset + yOffset * yOffset + zOffset * zOffset;
                    if (distance > SEARCH_RADIUS * SEARCH_RADIUS || distance >= closestDistance) {
                        continue;
                    }

                    cursor.set(origin.getX() + xOffset, origin.getY() + yOffset, origin.getZ() + zOffset);
                    if (this.fly.level().hasChunkAt(cursor)
                            && this.fly.level().getBlockState(cursor).is(ModBlocks.ANIMAL_DUNG.get())) {
                        closest = cursor.immutable();
                        closestDistance = distance;
                    }
                }
            }
        }

        return closest;
    }
}

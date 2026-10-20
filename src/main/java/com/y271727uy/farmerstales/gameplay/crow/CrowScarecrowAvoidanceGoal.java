package com.y271727uy.farmerstales.gameplay.crow;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/** Keeps crows outside the protected radius of tagged scarecrows. */
public final class CrowScarecrowAvoidanceGoal extends Goal {
    private static final TagKey<Block> SCARECROWS = TagKey.create(
        Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("farmerstales", "scarecrow")
    );
    private static final int PROTECTION_RADIUS = 5;
    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int FLEE_TICKS = 50;
    private static final double FLEE_DISTANCE = PROTECTION_RADIUS + 2.0D;
    private static final double FLEE_SPEED = 1.3D;

    private final Mob crow;
    @Nullable
    private BlockPos scarecrow;
    private int searchCooldown;
    private int fleeTicks;

    public CrowScarecrowAvoidanceGoal(Mob crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.crow.level().isClientSide || this.searchCooldown-- > 0) {
            return false;
        }
        this.searchCooldown = SEARCH_INTERVAL_TICKS;
        this.scarecrow = this.findNearbyScarecrow();
        return this.scarecrow != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.scarecrow != null
            && this.crow.level().getBlockState(this.scarecrow).is(SCARECROWS)
            && this.fleeTicks > 0;
    }

    @Override
    public void start() {
        this.fleeTicks = FLEE_TICKS;
        CrowFarmScareGoal.enableFlight(this.crow);
    }

    @Override
    public void tick() {
        if (this.scarecrow == null) {
            return;
        }
        this.fleeTicks--;
        CrowFarmScareGoal.enableFlight(this.crow);
        Vec3 away = this.crow.position().subtract(Vec3.atCenterOf(this.scarecrow));
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 destination = this.crow.position().add(away.normalize().scale(FLEE_DISTANCE)).add(0.0D, 2.0D, 0.0D);
        this.crow.getLookControl().setLookAt(destination.x, destination.y, destination.z);
        this.crow.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, FLEE_SPEED);
    }

    @Override
    public void stop() {
        this.scarecrow = null;
        this.fleeTicks = 0;
    }

    @Nullable
    private BlockPos findNearbyScarecrow() {
        BlockPos origin = this.crow.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int x = -PROTECTION_RADIUS; x <= PROTECTION_RADIUS; x++) {
            for (int y = -PROTECTION_RADIUS; y <= PROTECTION_RADIUS; y++) {
                for (int z = -PROTECTION_RADIUS; z <= PROTECTION_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!this.crow.level().hasChunkAt(cursor) || !this.crow.level().getBlockState(cursor).is(SCARECROWS)) {
                        continue;
                    }
                    double distance = this.crow.distanceToSqr(Vec3.atCenterOf(cursor));
                    if (distance < nearestDistance) {
                        nearest = cursor.immutable();
                        nearestDistance = distance;
                    }
                }
            }
        }
        return nearest;
    }
}

package com.y271727uy.farmerstales.gameplay.crow;

import java.util.Comparator;
import java.util.EnumSet;
import java.lang.reflect.Method;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.Vec3;

/** Makes crop-raiding crows flee nearby farmers before their crop AI can act. */
public final class CrowFarmScareGoal extends Goal {
    private static final int CROP_SEARCH_RADIUS = 8;
    private static final double DEFENDER_RANGE = 5.0D;
    private static final int SEARCH_INTERVAL_TICKS = 20;
    private static final int FLEE_TICKS = 40;
    private static final double FLEE_DISTANCE = 8.0D;
    private static final double FLEE_SPEED = 1.25D;

    private static final ClassValue<Optional<Method>> SET_FLYING = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("setFlying", boolean.class));
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        }
    };

    private final Mob crow;
    @Nullable
    private LivingEntity defender;
    private int searchCooldown;
    private int fleeTicks;

    public CrowFarmScareGoal(Mob crow) {
        this.crow = crow;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.crow.level().isClientSide || this.searchCooldown-- > 0) {
            return false;
        }
        this.searchCooldown = SEARCH_INTERVAL_TICKS;
        if (!this.hasCropNearby()) {
            return false;
        }
        this.defender = this.findNearestDefender();
        return this.defender != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.defender != null && this.defender.isAlive() && this.fleeTicks > 0;
    }

    @Override
    public void start() {
        this.fleeTicks = FLEE_TICKS;
        this.enableFlight();
        this.showVillagerAnger();
    }

    @Override
    public void tick() {
        if (this.defender == null) {
            return;
        }
        this.fleeTicks--;
        this.enableFlight();
        Vec3 away = this.crow.position().subtract(this.defender.position());
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(1.0D, 0.0D, 0.0D);
        }
        Vec3 destination = this.crow.position().add(away.normalize().scale(FLEE_DISTANCE)).add(0.0D, 2.0D, 0.0D);
        this.crow.getLookControl().setLookAt(destination.x, destination.y, destination.z);
        this.crow.getMoveControl().setWantedPosition(destination.x, destination.y, destination.z, FLEE_SPEED);
        if (this.fleeTicks % 10 == 0) {
            this.showVillagerAnger();
        }
    }

    @Override
    public void stop() {
        this.defender = null;
        this.fleeTicks = 0;
    }

    private boolean hasCropNearby() {
        BlockPos origin = this.crow.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -CROP_SEARCH_RADIUS; x <= CROP_SEARCH_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -CROP_SEARCH_RADIUS; z <= CROP_SEARCH_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (this.crow.level().hasChunkAt(cursor) && this.crow.level().getBlockState(cursor).getBlock() instanceof CropBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    private LivingEntity findNearestDefender() {
        return this.crow.level().getEntitiesOfClass(
            LivingEntity.class,
            this.crow.getBoundingBox().inflate(DEFENDER_RANGE),
            entity -> entity instanceof Player || entity instanceof Villager
        ).stream().min(Comparator.comparingDouble(this.crow::distanceToSqr)).orElse(null);
    }

    private void showVillagerAnger() {
        if (!(this.defender instanceof Villager) || !(this.crow.level() instanceof ServerLevel level)) {
            return;
        }
        level.sendParticles(
            ParticleTypes.ANGRY_VILLAGER,
            this.defender.getX(), this.defender.getY() + this.defender.getBbHeight() + 0.2D, this.defender.getZ(),
            3, 0.3D, 0.2D, 0.3D, 0.0D
        );
    }

    public static void enableFlight(Mob crow) {
        SET_FLYING.get(crow.getClass()).ifPresent(method -> {
            try {
                method.invoke(crow, true);
            } catch (ReflectiveOperationException ignored) {
                // A different crow implementation can safely fall back to its normal movement controller.
            }
        });
    }

    private void enableFlight() {
        enableFlight(this.crow);
    }
}

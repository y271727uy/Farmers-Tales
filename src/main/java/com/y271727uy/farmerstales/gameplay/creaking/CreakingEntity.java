package com.y271727uy.farmerstales.gameplay.creaking;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public final class CreakingEntity extends Monster {
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(CreakingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FROZEN = SynchedEntityData.defineId(CreakingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TEARING = SynchedEntityData.defineId(CreakingEntity.class, EntityDataSerializers.BOOLEAN);
    private BlockPos heartPos;
    private int teardownTicks;
    private int stuckWithPlayerTicks;
    private int attackTicks, swayTicks, hurtCooldown;
    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();

    public CreakingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setMaxUpStep(1.0625F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_OTHER, 0);
        setPathfindingMalus(BlockPathTypes.POWDER_SNOW, 0);
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 3).add(Attributes.FOLLOW_RANGE, 32);
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData(); entityData.define(ACTIVE, false); entityData.define(FROZEN, true); entityData.define(TEARING, false);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, false) {
            @Override public boolean canUse() { return canMove() && super.canUse(); }
            @Override public boolean canContinueToUse() { return canMove() && super.canContinueToUse(); }
            @Override public void tick() { if (canMove()) super.tick(); }
        });
    }
    public void bindToHeart(BlockPos pos) { heartPos = pos.immutable(); setPersistenceRequired(); }
    public BlockPos heartPosition() { return heartPos; }
    public boolean isActivated() { return entityData.get(ACTIVE); }
    public boolean isFrozen() { return entityData.get(FROZEN); }
    public boolean isTearingDown() { return entityData.get(TEARING); }
    public boolean canMove() { return isActivated() && !isFrozen() && !isTearingDown(); }

    /** Includes line-of-sight and disguise checks; all nearby survival observers count. */
    public boolean isWatchedBy(Player player) {
        if (!eligible(player) || isAlliedTo(player) || player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) return false;
        Vec3 view = player.getViewVector(1).normalize();
        for (double y : new double[]{getEyeY(), getY() + 0.5, (getEyeY() + getY()) / 2}) {
            Vec3 point = new Vec3(getX(), y, getZ());
            Vec3 towards = point.subtract(player.getEyePosition()).normalize();
            if (view.dot(towards) > 0.5 && level().clip(new net.minecraft.world.level.ClipContext(
                    player.getEyePosition(), point, net.minecraft.world.level.ClipContext.Block.VISUAL,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, player)).getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;
        }
        return false;
    }
    private static boolean eligible(Player player) { return player.isAlive() && !player.isCreative() && !player.isSpectator(); }

    @Override public void tick() {
        if (!level().isClientSide) {
            if (hurtCooldown > 0) hurtCooldown--;
            if (isTearingDown()) {
                getNavigation().stop(); setTarget(null);
                if (--teardownTicks <= 0) {
                    ((ServerLevel)level()).sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                            net.minecraft.core.particles.ParticleTypes.BLOCK,
                            com.y271727uy.farmerstales.all.ForestBlocks.PALE_OAK_WOOD.get().defaultBlockState()),
                            getX(), getY() + getBbHeight() / 2, getZ(), 100, 0.3, 0.8, 0.3, 0.05);
                    discard(); return;
                }
            } else {
                if (heartPos != null && level().hasChunkAt(heartPos)) {
                    if (!(level().getBlockEntity(heartPos) instanceof CreakingHeartBlockEntity heart) || !heart.owns(getUUID())
                            || distanceToSqr(Vec3.atCenterOf(heartPos)) > 34 * 34) beginTearDown();
                }
                var players = level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(32), p -> eligible(p) && !isAlliedTo(p) && distanceToSqr(p) <= 1024);
                boolean watched = players.stream().anyMatch(this::isWatchedBy);
                boolean active = isActivated() && !players.isEmpty()
                        || players.stream().anyMatch(player -> distanceToSqr(player) <= 144 && isWatchedBy(player));
                if (active != isActivated()) playSound(CreakingContent.sound(active ? "entity.creaking.activate" : "entity.creaking.deactivate"), 1, 1);
                entityData.set(ACTIVE, active);
                boolean wasFrozen = isFrozen();
                entityData.set(FROZEN, watched || !active || isTearingDown()
                        || heartPos != null && !level().hasChunkAt(heartPos));
                if (active && wasFrozen != isFrozen()) playSound(CreakingContent.sound(isFrozen() ? "entity.creaking.freeze" : "entity.creaking.unfreeze"), 1, 1);
                setTarget(active ? players.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null) : null);
                if (players.stream().anyMatch(p -> p.getBoundingBox().intersects(getBoundingBox()))) stuckWithPlayerTicks++;
                else stuckWithPlayerTicks = 0;
                if (heartPos != null && stuckWithPlayerTicks > 100) beginTearDown();
            }
            if (!canMove()) { getNavigation().stop(); setDeltaMovement(0, getDeltaMovement().y, 0); }
        }
        super.tick();
        if (level().isClientSide) {
            if (attackTicks > 0 && --attackTicks == 0) attackAnimationState.stop();
            if (swayTicks > 0 && --swayTicks == 0) invulnerabilityAnimationState.stop();
            if (isTearingDown()) deathAnimationState.startIfStopped(tickCount);
        }
    }

    public void beginTearDown() {
        if (isTearingDown()) return;
        entityData.set(TEARING, true); entityData.set(FROZEN, true); teardownTicks = 46;
        playSound(CreakingContent.sound("entity.creaking.death"), 1, 1);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.world.damagesource.DamageTypes.CACTUS) || source.is(net.minecraft.world.damagesource.DamageTypes.SWEET_BERRY_BUSH)) return false;
        if (heartPos == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (!level().isClientSide && !isTearingDown() && hurtCooldown == 0
                && (source.getDirectEntity() instanceof LivingEntity || source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile)) {
            hurtCooldown = 8;
            level().broadcastEntityEvent(this, (byte) 61);
            playSound(CreakingContent.sound("entity.creaking.sway"), 1, 1);
            if (source.getEntity() instanceof Player || source.getEntity() instanceof TamableAnimal pet && pet.isTame()) {
                if (level().getBlockEntity(heartPos) instanceof CreakingHeartBlockEntity heart) heart.onProtectorHit(this);
            }
            return true;
        }
        return false;
    }
    @Override public boolean doHurtTarget(Entity target) {
        if (!canMove()) return false;
        boolean hit = super.doHurtTarget(target);
        if (hit) { level().broadcastEntityEvent(this, (byte) 4); playSound(CreakingContent.sound("entity.creaking.attack"), 1, 1); }
        return hit;
    }
    @Override public void handleEntityEvent(byte id) {
        if (id == 4) { attackTicks = 16; attackAnimationState.start(tickCount); }
        else if (id == 61) { swayTicks = 8; invulnerabilityAnimationState.start(tickCount); }
        else super.handleEntityEvent(id);
    }
    @Override protected boolean isImmobile() { return !canMove() || super.isImmobile(); }
    @Override public boolean canFreeze() { return false; }
    @Override public boolean isPushable() { return canMove() && super.isPushable(); }
    @Override public void push(double x, double y, double z) { if (canMove()) super.push(x, y, z); }
    @Override public void knockback(double strength, double x, double z) { if (canMove()) super.knockback(strength, x, z); }
    @Override public boolean canChangeDimensions() { return heartPos == null && super.canChangeDimensions(); }
    @Override protected boolean canRide(Entity entity) { return false; }
    @Override protected void updateWalkAnimation(float distance) { walkAnimation.update(Math.min(distance * 25, 3), 0.4F); }
    @Override protected SoundEvent getAmbientSound() { return isActivated() ? null : CreakingContent.sound("entity.creaking.ambient"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return CreakingContent.sound("entity.creaking.sway"); }
    @Override protected SoundEvent getDeathSound() { return CreakingContent.sound("entity.creaking.death"); }
    @Override protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(CreakingContent.sound("entity.creaking.step"), 0.15F, 1);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (heartPos != null) tag.put("CreakingHeart", NbtUtils.writeBlockPos(heartPos));
        tag.putBoolean("CreakingActive", isActivated());
        tag.putInt("TearDownTicks", teardownTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CreakingHeart")) heartPos = NbtUtils.readBlockPos(tag.getCompound("CreakingHeart"));
        entityData.set(ACTIVE, tag.getBoolean("CreakingActive"));
        teardownTicks = tag.getInt("TearDownTicks"); entityData.set(TEARING, teardownTicks > 0);
    }
}

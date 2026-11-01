package com.y271727uy.farmerstales.gameplay.creaking;

import com.y271727uy.farmerstales.all.ForestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.*;

public final class CreakingHeartBlockEntity extends BlockEntity {
    private UUID protector;
    private BlockPos lastProtectorPosition;
    private long resinReadyAt;
    private int age;
    private int output;

    public CreakingHeartBlockEntity(BlockPos pos, BlockState state) { super(CreakingContent.HEART_ENTITY.get(), pos, state); }
    public UUID protectorId() { return protector; }
    public boolean owns(UUID id) { return id.equals(protector); }
    public int outputSignal() { return output; }
    public CreakingEntity protector() {
        return level instanceof ServerLevel server && protector != null && server.getEntity(protector) instanceof CreakingEntity entity ? entity : null;
    }
    public static boolean naturalNight(Level level) {
        return level.dimensionType().natural() && Math.floorMod(level.getDayTime(), 24000L) >= 12000;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CreakingHeartBlockEntity heart) {
        if (!(level instanceof ServerLevel server)) return;
        heart.age++;
        if (heart.age % 20 != 0) return;
        boolean aligned = CreakingHeartBlock.hasAlignedLogs(level, pos, state);
        boolean night = naturalNight(level);
        var phase = !aligned ? CreakingHeartBlock.HeartState.UPROOTED : night
                ? CreakingHeartBlock.HeartState.AWAKE : CreakingHeartBlock.HeartState.DORMANT;
        if (state.getValue(CreakingHeartBlock.STATE) != phase) level.setBlock(pos, state.setValue(CreakingHeartBlock.STATE, phase), 3);
        var entity = heart.protector();
        if (entity != null && (entity.isRemoved() || entity.isTearingDown())) { heart.clearProtector(); entity = null; }
        if (!aligned || !night && (entity == null || !entity.hasCustomName()) || level.getDifficulty() == Difficulty.PEACEFUL) {
            heart.removeProtector();
        } else if (entity != null) {
            heart.lastProtectorPosition = entity.blockPosition();
            if (entity.distanceToSqr(Vec3.atCenterOf(pos)) > 34 * 34) heart.removeProtector();
        } else if (heart.protector != null) {
            // Do not spawn a duplicate while its entity chunk is unloaded. On a
            // reload allow entity loading to finish before treating a UUID as lost.
            if (heart.age > 100 && (heart.lastProtectorPosition == null || server.isPositionEntityTicking(heart.lastProtectorPosition))) heart.clearProtector();
        } else if (night && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32,
                player -> player.isAlive() && !player.isSpectator()) != null) {
            heart.trySpawn(server);
        }
        entity = heart.protector();
        int signal = entity == null ? 0 : Math.max(0, 15 - (int) Math.floor(Math.sqrt(entity.distanceToSqr(Vec3.atCenterOf(pos))) / 32 * 15));
        if (signal != heart.output) { heart.output = signal; level.updateNeighbourForOutputSignal(pos, state.getBlock()); }
        if (phase == CreakingHeartBlock.HeartState.AWAKE && server.random.nextInt(5) == 0) {
            server.playSound(null, pos, CreakingContent.sound("block.creaking_heart.idle"), SoundSource.AMBIENT, 0.3F, 1);
        }
        heart.setChanged();
    }

    public boolean trySpawn(ServerLevel server) {
        if (protector != null || !naturalNight(server) || server.getDifficulty() == Difficulty.PEACEFUL
                || !CreakingHeartBlock.hasAlignedLogs(server, worldPosition, getBlockState())) return false;
        for (int attempt = 0; attempt < 8; attempt++) {
            int x = worldPosition.getX() + server.random.nextInt(33) - 16;
            int z = worldPosition.getZ() + server.random.nextInt(33) - 16;
            for (int dy = 8; dy >= -8; dy--) {
                var pos = new BlockPos(x, worldPosition.getY() + dy, z);
                if (!server.hasChunkAt(pos) || !server.getBlockState(pos.below()).isFaceSturdy(server, pos.below(), Direction.UP)) continue;
                var entity = CreakingContent.CREAKING.get().create(server);
                if (entity == null) return false;
                entity.moveTo(x + 0.5, pos.getY(), z + 0.5, server.random.nextFloat() * 360, 0);
                if (!server.noCollision(entity) || server.containsAnyLiquid(entity.getBoundingBox())) continue;
                entity.bindToHeart(worldPosition);
                entity.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null, null);
                protector = entity.getUUID(); lastProtectorPosition = pos;
                if (server.addFreshEntity(entity)) {
                    server.playSound(null, worldPosition, CreakingContent.sound("block.creaking_heart.spawn"), SoundSource.BLOCKS, 1, 1);
                    entity.playSound(CreakingContent.sound("entity.creaking.spawn"), 1, 1);
                    setChanged(); return true;
                }
                clearProtector();
            }
        }
        return false;
    }

    public void removeProtector() {
        var entity = protector();
        if (entity != null) entity.beginTearDown();
        clearProtector();
    }
    private void clearProtector() {
        protector = null; lastProtectorPosition = null;
        if (output != 0 && level != null) {
            output = 0;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        setChanged();
    }

    public void onProtectorHit(CreakingEntity entity) {
        if (!(level instanceof ServerLevel server) || !owns(entity.getUUID()) || !naturalNight(server)) return;
        var start = entity.getBoundingBox().getCenter();
        var end = Vec3.atCenterOf(worldPosition);
        for (int i = 0; i < 16; i++) {
            var point = start.lerp(end, i / 15.0);
            server.sendParticles(new DustParticleOptions(new Vector3f(0.95F, 0.4F, 0.08F), 0.8F), point.x, point.y, point.z, 1, 0.03, 0.03, 0.03, 0);
        }
        server.playSound(null, worldPosition, CreakingContent.sound("block.creaking_heart.hurt"), SoundSource.BLOCKS, 1, 1);
        if (server.getGameTime() < resinReadyAt) return;
        int remaining = 2 + server.random.nextInt(2);
        var queue = new ArrayDeque<BlockPos>(); var visited = new HashSet<BlockPos>();
        queue.add(worldPosition); visited.add(worldPosition);
        var resin = (ResinClumpBlock) CreakingContent.RESIN_CLUMP.get();
        while (!queue.isEmpty() && visited.size() <= 64 && remaining > 0) {
            var log = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                var neighbor = log.relative(direction);
                if (!server.hasChunkAt(neighbor)) continue;
                var current = server.getBlockState(neighbor);
                if (current.is(ForestBlocks.PALE_OAK_LOG.get()) && neighbor.distManhattan(worldPosition) <= 3 && visited.add(neighbor)) queue.add(neighbor);
            }
            if (log.equals(worldPosition)) continue;
            int shift = server.random.nextInt(6);
            for (int i = 0; i < 6 && remaining > 0; i++) {
                var direction = Direction.values()[(i + shift) % 6];
                var target = log.relative(direction);
                var current = server.getBlockState(target);
                if (!current.isAir() && !current.is(resin) && !current.getFluidState().is(FluidTags.WATER)) continue;
                var placed = resin.getStateForPlacement(current, server, target, direction.getOpposite());
                if (placed != null) {
                    server.setBlockAndUpdate(target, placed);
                    server.gameEvent(GameEvent.BLOCK_PLACE, target, GameEvent.Context.of(entity, placed));
                    remaining--;
                }
            }
        }
        resinReadyAt = server.getGameTime() + 100;
        setChanged();
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (protector != null) tag.putUUID("Protector", protector);
        if (lastProtectorPosition != null) tag.put("ProtectorPosition", NbtUtils.writeBlockPos(lastProtectorPosition));
        tag.putLong("ResinReadyAt", resinReadyAt);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        protector = tag.hasUUID("Protector") ? tag.getUUID("Protector") : null;
        lastProtectorPosition = tag.contains("ProtectorPosition") ? NbtUtils.readBlockPos(tag.getCompound("ProtectorPosition")) : null;
        resinReadyAt = tag.getLong("ResinReadyAt");
    }
}

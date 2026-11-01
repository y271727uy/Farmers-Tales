package com.y271727uy.farmerstales.gameplay.environment.forest.client;

import com.y271727uy.farmerstales.FTMod;
import com.y271727uy.farmerstales.all.ForestBlocks;
import com.y271727uy.farmerstales.gameplay.environment.forest.ForestLeafParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FTMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ForestLeafParticleClient {
    private ForestLeafParticleClient() {}

    @SubscribeEvent
    public static void providers(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ForestLeafParticles.TINTED.get(), sprites -> new FallingLeafParticle.Provider(sprites, true));
        event.registerSpriteSet(ForestLeafParticles.PALE_OAK.get(), sprites -> new FallingLeafParticle.Provider(sprites, false));
        event.registerSpriteSet(ForestLeafParticles.RED_POPLAR.get(), sprites -> new FallingLeafParticle.Provider(sprites, false));
        event.registerSpriteSet(ForestLeafParticles.ORANGE_POPLAR.get(), sprites -> new FallingLeafParticle.Provider(sprites, false));
        event.registerSpriteSet(ForestLeafParticles.YELLOW_POPLAR.get(), sprites -> new FallingLeafParticle.Provider(sprites, false));
    }

    private static SimpleParticleType particle(BlockState state) {
        if (state.is(ForestBlocks.PALE_OAK_LEAVES.get())) return ForestLeafParticles.PALE_OAK.get();
        if (state.is(ForestBlocks.RED_POPLAR_LEAVES.get())) return ForestLeafParticles.RED_POPLAR.get();
        if (state.is(ForestBlocks.ORANGE_POPLAR_LEAVES.get())) return ForestLeafParticles.ORANGE_POPLAR.get();
        if (state.is(ForestBlocks.YELLOW_POPLAR_LEAVES.get())) return ForestLeafParticles.YELLOW_POPLAR.get();
        if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES)
                || state.is(Blocks.ACACIA_LEAVES) || state.is(Blocks.DARK_OAK_LEAVES) || state.is(Blocks.MANGROVE_LEAVES)
                || state.is(Blocks.AZALEA_LEAVES) || state.is(Blocks.FLOWERING_AZALEA_LEAVES)) return ForestLeafParticles.TINTED.get();
        // Cherry already emits in 1.20.1; 26.3 removes falling leaves from spruce.
        // Other mods keep control over their own tree effects.
        return null;
    }

    public static void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        var type = particle(state);
        if (type == null || !level.getFluidState(pos).isEmpty() || random.nextFloat() >= 0.01F) return;
        var below = pos.below();
        if (Block.isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP)) return;
        if (!level.getFluidState(below).isEmpty()) return;
        ParticleUtils.spawnParticleBelow(level, pos, random, type);
    }
}

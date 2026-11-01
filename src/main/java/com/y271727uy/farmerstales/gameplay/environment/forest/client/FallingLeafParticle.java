package com.y271727uy.farmerstales.gameplay.environment.forest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.LeavesBlock;

/** A light, wind-driven leaf adapted to 1.20.1's particle collision API. */
public final class FallingLeafParticle extends TextureSheetParticle {
    private final float phase;
    private final float spin;

    private FallingLeafParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 200 + random.nextInt(101);
        quadSize = random.nextBoolean() ? 0.05F : 0.075F;
        setSize(0.04F, 0.04F);
        phase = random.nextFloat() * (float) (Math.PI * 2);
        spin = (random.nextBoolean() ? 1 : -1) * (0.025F + random.nextFloat() * 0.025F);
        roll = oRoll = phase;
        yd = -0.021;
        gravity = 0.0007F;
        hasPhysics = true;
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }

    @Override public void tick() {
        xo = x; yo = y; zo = z; oRoll = roll;
        if (age++ >= lifetime) { remove(); return; }
        roll += spin;
        double time = age / 20.0;
        xd = Math.cos(phase) * 0.018 + Math.sin(time + phase) * 0.012;
        zd = Math.sin(phase) * 0.018 + Math.cos(time * 0.8 + phase) * 0.012;
        yd = Math.max(yd - gravity, -0.07);
        move(xd, yd, zd);
        if (onGround || age > 2 && y == yo) remove();
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final boolean tinted;

        public Provider(SpriteSet sprites, boolean tinted) { this.sprites = sprites; this.tinted = tinted; }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            var particle = new FallingLeafParticle(level, x, y, z, sprites);
            if (tinted) {
                // ParticleUtils emits just below the source block. Sample its leaf
                // tint now so seasonal/resource-pack coloring remains attached in flight.
                var source = BlockPos.containing(x, y + 0.1, z);
                var state = level.getBlockState(source);
                int color = state.getBlock() instanceof LeavesBlock
                        ? Minecraft.getInstance().getBlockColors().getColor(state, level, source, 0)
                        : FoliageColor.getDefaultColor();
                if (color == -1) color = FoliageColor.getDefaultColor();
                particle.setColor((color >> 16 & 255) / 255.0F, (color >> 8 & 255) / 255.0F, (color & 255) / 255.0F);
            }
            return particle;
        }
    }
}

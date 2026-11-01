package com.y271727uy.farmerstales.gameplay.environment;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public final class FireflyParticle extends TextureSheetParticle {
    private final float phase;

    private FireflyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        pickSprite(sprites);
        lifetime = 40 + random.nextInt(40);
        phase = random.nextFloat() * (float) Math.PI * 2;
        quadSize = 0.025F;
        setSize(0.01F, 0.01F);
        hasPhysics = false;
        setColor(1, 1, 1);
        setAlpha(0);
    }

    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    @Override protected int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) { remove(); return; }
        double time = age * 0.08;
        move(Math.sin(time + phase) * 0.012, Math.cos(time * 0.7 + phase) * 0.006, Math.cos(time + phase) * 0.012);
        float envelope = Math.min(1, Math.min(age / 8.0F, (lifetime - age) / 8.0F));
        setAlpha(envelope * (0.55F + 0.45F * (float) Math.sin(time * 2 + phase)));
    }

    public static final class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites = sprites; }
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                double dx, double dy, double dz) { return new FireflyParticle(level, x, y, z, sprites); }
    }
}

package com.y271727uy.farmerstales.gameplay.curated_crop.client;

import java.awt.Color;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Motion copied from tools/curated_crop-preview. The preview uses pixels around a
 * 92px-tall crop; here one pixel is 1/92 of a block so the same paths fit a crop.
 */
public class CuratedCropParticle extends TextureSheetParticle {
    private static final float SCALE = 1f / 92f;
    private final Kind kind;
    private final double anchorX;
    private final double anchorY;
    private final double anchorZ;
    private final float radius;
    private final float baseSize;
    private final float fromRed;
    private final float fromGreen;
    private final float fromBlue;
    private final float toRed;
    private final float toGreen;
    private final float toBlue;
    private float angle;
    private float along;
    private float flap;

    public CuratedCropParticle(ClientLevel level, double x, double y, double z, Kind kind, SpriteSet sprites) {
        super(level, x, y, z);
        this.kind = kind;
        this.anchorX = x;
        this.anchorY = y;
        this.anchorZ = z;
        this.angle = random.nextFloat() * Mth.TWO_PI;
        this.along = random.nextFloat();
        this.flap = random.nextFloat() * Mth.TWO_PI;
        this.radius = (34f + random.nextFloat() * 8f) * SCALE;
        this.lifetime = switch (kind) {
            case STARS -> 40 + random.nextInt(20);
            case RAINBOW -> 160 + random.nextInt(40);
            case POPULAR -> 200 + random.nextInt(60);
            case MOON -> 240 + random.nextInt(80);
        };
        this.baseSize = kind == Kind.POPULAR ? 0.11f : 0.07f;
        this.quadSize = baseSize;
        this.fromRed = kind.red;
        this.fromGreen = kind.green;
        this.fromBlue = kind.blue;
        this.toRed = kind.toRed;
        this.toGreen = kind.toGreen;
        this.toBlue = kind.toBlue;
        this.hasPhysics = false;
        this.gravity = 0f;
        pickSprite(sprites);
        setColor(fromRed, fromGreen, fromBlue);
        if (kind == Kind.STARS) {
            setPos(
                    x + (random.nextDouble() - 0.5) * 28f * SCALE,
                    y + (46f + (random.nextDouble() - 0.5) * 36f) * SCALE,
                    z + (random.nextDouble() - 0.5) * 28f * SCALE
            );
            xo = x;
            yo = y;
            zo = z;
        } else {
            place(0f);
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float step = 1f / 20f;
        switch (kind) {
            case MOON -> angle += step * 0.35f;
            case RAINBOW -> along = (along + step * 0.12f) % 1f;
            case POPULAR -> {
                angle += step * 0.8f;
                flap += step * 9f;
            }
            case STARS -> { }
        }
        place(age * step);
        tint(age * step);
    }

    private void tint(float ageSeconds) {
        float progress = Mth.clamp((float) age / (float) lifetime, 0f, 1f);
        if (kind == Kind.RAINBOW) {
            int rgb = Color.HSBtoRGB(along * 300f / 360f, 0.8f * (1f - progress * 0.35f), 0.78f + progress * 0.18f);
            setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f);
        } else if (kind == Kind.STARS) {
            float flash = Mth.sin(ageSeconds * 18f) > 0f ? 1f : 0.35f;
            setColor(
                    Mth.lerp(flash, fromRed, toRed),
                    Mth.lerp(flash, fromGreen, toGreen),
                    Mth.lerp(flash, fromBlue, toBlue)
            );
            setAlpha(0.35f + flash * 0.65f);
        } else {
            setColor(
                    Mth.lerp(progress, fromRed, toRed),
                    Mth.lerp(progress, fromGreen, toGreen),
                    Mth.lerp(progress, fromBlue, toBlue)
            );
        }
        if (kind != Kind.STARS) {
            setAlpha(progress < 0.5f ? 1f : 1f - (progress - 0.5f) * 2f);
        }
        quadSize = baseSize * (1f - progress * 0.35f);
    }

    private void place(float ageSeconds) {
        switch (kind) {
            case MOON -> {
                x = anchorX + Mth.cos(angle) * radius;
                y = anchorY + 4f * SCALE + Mth.sin(ageSeconds * 1.4f) * 2f * SCALE;
                z = anchorZ + Mth.sin(angle) * radius;
            }
            case RAINBOW -> {
                float span = 92f * SCALE;
                x = anchorX - span / 2f + along * span;
                y = anchorY + (92f - Mth.sin(along * Mth.PI) * 28f) * SCALE;
                z = anchorZ;
            }
            case POPULAR -> {
                float orbit = 42f * SCALE;
                x = anchorX + Mth.cos(angle) * orbit + Mth.sin(flap) * 4f * SCALE;
                y = anchorY + 46f * SCALE + Mth.sin(angle * 2f) * 10f * SCALE;
                z = anchorZ + Mth.sin(angle) * orbit;
            }
            case STARS -> {
                x += Mth.sin(ageSeconds * 6f) * 4f * SCALE * 0.05f;
                y -= 0.12f * 20f * SCALE * 0.05f;
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public enum Kind {
        MOON(0.957f, 0.945f, 0.910f, 0.620f, 0.718f, 0.847f),
        RAINBOW(1f, 1f, 1f, 1f, 1f, 1f),
        POPULAR(0.941f, 0.757f, 0.290f, 1f, 0.957f, 0.761f),
        STARS(0.725f, 0.627f, 1f, 0.937f, 0.910f, 1f);

        final float red;
        final float green;
        final float blue;
        final float toRed;
        final float toGreen;
        final float toBlue;

        Kind(float red, float green, float blue, float toRed, float toGreen, float toBlue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.toRed = toRed;
            this.toGreen = toGreen;
            this.toBlue = toBlue;
        }
    }

    public static final class Provider implements net.minecraft.client.particle.ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Kind kind;

        public Provider(SpriteSet sprites, Kind kind) {
            this.sprites = sprites;
            this.kind = kind;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new CuratedCropParticle(level, x, y, z, kind, sprites);
        }
    }
}

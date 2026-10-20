package com.y271727uy.farmerstales.mixin.stellarview;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.y271727uy.farmerstales.gameplay.special_event.common.MoonPhaseClientVisuals;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.stellarview.common.util.TextureLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stellar View draws its own moon from a {@link TextureLayer} and never reads
 * {@code LevelRenderer.MOON_LOCATION}, so the vanilla texture swap is skipped.
 * Only the ordinary moon atlas is replaced; the halo layer keeps its own texture.
 */
@Pseudo
@Mixin(targets = "net.povstalec.stellarview.common.util.TextureLayer", remap = false)
public abstract class TextureLayerMoonTextureMixin {
    @WrapOperation(
            method = "texture",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/povstalec/stellarview/common/util/TextureLayer;texture:Lnet/minecraft/resources/ResourceLocation;",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private ResourceLocation farmerstales$swapMoonTexture(TextureLayer layer, Operation<ResourceLocation> original) {
        ResourceLocation texture = original.call(layer);
        ResourceLocation replacement = MoonPhaseClientVisuals.activeMoonTexture();
        if (replacement == null || texture == null || !isVanillaMoonAtlas(texture)) {
            return texture;
        }
        return replacement;
    }

    /** Covers both the vanilla-moon toggle and Stellar View's own luna atlas. */
    private static boolean isVanillaMoonAtlas(ResourceLocation texture) {
        if (!"textures/environment/moon_phases.png".equals(texture.getPath())) {
            return false;
        }
        String namespace = texture.getNamespace();
        return "minecraft".equals(namespace) || "stellarview".equals(namespace);
    }
}

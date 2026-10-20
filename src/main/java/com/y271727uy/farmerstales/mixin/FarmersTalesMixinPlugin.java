package com.y271727uy.farmerstales.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Single mixin config gate. Optional integrations live in the same json and
 * apply only when every required mod is present.
 */
public final class FarmersTalesMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "com.y271727uy.farmerstales.mixin.";
    private static final List<OptionalMixin> OPTIONAL_MIXINS = List.of(
            new OptionalMixin("alexmob.", "alexsmobs"),
            new OptionalMixin("farmandcharm.", "farm_and_charm"),
            new OptionalMixin("forestry.", "forestry"),
            new OptionalMixin("stellarview.", "stellarview")
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(MIXIN_PACKAGE)) {
            return true;
        }
        String relativeName = mixinClassName.substring(MIXIN_PACKAGE.length());
        for (OptionalMixin optional : OPTIONAL_MIXINS) {
            if (!relativeName.startsWith(optional.packagePrefix())) {
                continue;
            }
            for (String modId : optional.modIds()) {
                if (!isModLoaded(modId)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean isModLoaded(String modId) {
        try {
            return FMLLoader.getLoadingModList().getModFileById(modId) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private record OptionalMixin(String packagePrefix, String... modIds) {
    }
}

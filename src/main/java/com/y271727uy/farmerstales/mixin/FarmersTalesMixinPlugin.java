package com.y271727uy.farmerstales.mixin;

import com.y271727uy.farmerstales.backport.BackportConfig;
import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Single mixin config gate. Optional integrations live in the same json and
 * apply only when every required mod is present. Mixins annotated with
 * {@code @Backport} apply only when that backport feature is enabled.
 */
public final class FarmersTalesMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE = "com.y271727uy.farmerstales.mixin.";
    private static final String BACKPORT_DESCRIPTOR = "Lcom/y271727uy/farmerstales/backport/Backport;";
    private static final List<OptionalMixin> OPTIONAL_MIXINS = List.of(
            new OptionalMixin("alexmob.", "alexsmobs"),
            new OptionalMixin("farmandcharm.", "farm_and_charm"),
            new OptionalMixin("ftblibrary.", "ftblibrary"),
            new OptionalMixin("forestry.", "forestry"),
            new OptionalMixin("stellarview.", "stellarview"),
            new OptionalMixin("xaero.", "xaerominimap")
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
        if (!backportEnabled(mixinClassName)) {
            return false;
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

    /** Reads {@code @Backport} from the mixin's bytecode; the class itself must not be loaded. */
    private static boolean backportEnabled(String mixinClassName) {
        ClassNode node;
        try {
            node = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName);
        } catch (Exception exception) {
            return true;
        }
        if (node.visibleAnnotations == null) {
            return true;
        }
        for (AnnotationNode annotation : node.visibleAnnotations) {
            if (!BACKPORT_DESCRIPTOR.equals(annotation.desc) || annotation.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("value".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof String[] enumValue) {
                    return BackportConfig.isEnabled(enumValue[1]);
                }
            }
        }
        return true;
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

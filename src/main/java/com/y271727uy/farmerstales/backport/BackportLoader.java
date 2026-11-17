package com.y271727uy.farmerstales.backport;

import com.y271727uy.farmerstales.FTMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.forgespi.language.ModFileScanData;
import net.minecraftforge.forgespi.locating.IModFile;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Registers {@link Backport}-annotated classes and data packs whose feature is enabled. */
public final class BackportLoader {
    public static final String PACK_ROOT = "backports";
    private static final Type BACKPORT = Type.getType(Backport.class);
    private static final String MIXIN_PACKAGE = "com.y271727uy.farmerstales.mixin.";

    private BackportLoader() {}

    public static void init(IEventBus modBus) {
        FTMod.LOGGER.info("Farmer's Tales backports enabled: {}", BackportConfig.enabled());
        FTMod.LOGGER.warn("Farmer's Tales: back-ported content is off by default and controlled by config/{}. Changing it "
                + "for an existing world deletes the blocks, items and entities of disabled features from that world; back up first.",
                BackportConfig.FILE_NAME);
        IModFile modFile = ModList.get().getModFileById(FTMod.MODID).getFile();
        List<ModFileScanData.AnnotationData> annotated = new ArrayList<>();
        for (var data : modFile.getScanResult().getAnnotations()) {
            if (BACKPORT.equals(data.annotationType()) && data.targetType() == ElementType.TYPE) annotated.add(data);
        }
        annotated.sort(Comparator.comparing(data -> data.clazz().getClassName()));
        for (var data : annotated) load(data, modBus);
        modBus.addListener((AddPackFindersEvent event) -> addPacks(event, modFile));
    }

    private static void load(ModFileScanData.AnnotationData data, IEventBus modBus) {
        Map<String, Object> values = data.annotationData();
        BackportFeature feature = BackportFeature.byId(enumValue(values.get("value")));
        if (feature == null || !feature.isEnabled()) return;
        if (Boolean.TRUE.equals(values.get("clientOnly")) && !FMLEnvironment.dist.isClient()) return;

        String className = data.clazz().getClassName();
        // Mixins are handled by FarmersTalesMixinPlugin and must never be loaded directly.
        if (className.startsWith(MIXIN_PACKAGE)) return;
        Class<?> type;
        try {
            type = Class.forName(className, true, BackportLoader.class.getClassLoader());
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot load backport class " + className, exception);
        }

        Method register = registerMethod(type);
        if (register != null) {
            try {
                register.invoke(null, modBus);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot register backport content " + className, exception);
            }
        }
        if (values.get("subscribe") instanceof Collection<?> buses) {
            for (Object bus : buses) {
                switch (Backport.Bus.valueOf(enumValue(bus))) {
                    case FORGE -> MinecraftForge.EVENT_BUS.register(type);
                    case MOD -> modBus.register(type);
                }
            }
        }
    }

    private static Method registerMethod(Class<?> type) {
        try {
            Method method = type.getMethod("register", IEventBus.class);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static String enumValue(Object value) {
        if (value instanceof net.minecraftforge.fml.loading.moddiscovery.ModAnnotation.EnumHolder holder) return holder.getValue();
        return value == null ? null : value.toString();
    }

    /**
     * Each directory under {@code backports/} is a data pack named after the features it needs,
     * joined with {@code +} (for example {@code shelf+pale_garden}).
     */
    private static void addPacks(AddPackFindersEvent event, IModFile modFile) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        Path root = modFile.findResource(PACK_ROOT);
        if (!Files.isDirectory(root)) return;
        List<Path> directories;
        try (var stream = Files.list(root)) {
            directories = stream.filter(Files::isDirectory).sorted().toList();
        } catch (IOException exception) {
            FTMod.LOGGER.error("Cannot list Farmer's Tales backport data packs", exception);
            return;
        }
        for (Path directory : directories) {
            String name = directory.getFileName().toString().replace("/", "");
            if (!packEnabled(name)) continue;
            String id = FTMod.MODID + ":" + PACK_ROOT + "/" + name;
            event.addRepositorySource(consumer -> {
                Pack pack = Pack.readMetaAndCreate(id, Component.literal("Farmer's Tales backport: " + name), true,
                        packId -> new PathPackResources(packId, directory, true),
                        PackType.SERVER_DATA, Pack.Position.TOP, PackSource.BUILT_IN);
                if (pack != null) consumer.accept(pack);
            });
        }
    }

    private static boolean packEnabled(String name) {
        for (String id : name.split("\\+")) {
            var feature = BackportFeature.byId(id);
            if (feature == null) {
                FTMod.LOGGER.warn("Unknown backport feature '{}' in data pack {}", id, name);
                return false;
            }
            if (!feature.isEnabled()) return false;
        }
        return true;
    }
}

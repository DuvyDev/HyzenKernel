package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - WorldConfig SpawnProvider Transformer
 *
 * Ensures WorldConfig.setSpawnProvider() marks the config as changed so
 * spawn providers are persisted to disk (prevents return portal drift).
 */
public class WorldConfigSpawnProviderTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.core.universe.world.WorldConfig";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        if (!EarlyConfigManager.getInstance().isTransformerEnabled("staticSharedInstances")) {
            info("WorldConfigSpawnProviderTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming WorldConfig...");
        verbose("Marking config dirty on setSpawnProvider()");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new WorldConfigSpawnProviderVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("WorldConfig SpawnProvider transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform WorldConfig!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

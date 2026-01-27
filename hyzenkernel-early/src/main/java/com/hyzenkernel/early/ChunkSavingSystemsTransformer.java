package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - ChunkSavingSystems transformer
 *
 * Skips saving already-on-disk chunks for shared portal instances
 * (instance-shared-*) so only new chunks are persisted.
 */
public class ChunkSavingSystemsTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems";

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
            info("ChunkSavingSystemsTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming ChunkSavingSystems...");
        verbose("Skipping saves for shared instance chunks already on disk");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new ChunkSavingSystemsVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("ChunkSavingSystems transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform ChunkSavingSystems!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

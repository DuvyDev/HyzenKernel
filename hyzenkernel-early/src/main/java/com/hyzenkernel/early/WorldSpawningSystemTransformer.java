package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - WorldSpawningSystem Bytecode Transformer
 *
 * Fixes sporadic Invalid entity reference crashes when chunk refs become
 * invalid during WorldSpawningSystem.pickRandomChunk() while chunks unload.
 *
 * The Fix:
 * Wrap pickRandomChunk() in a try-catch for IllegalStateException and return null
 * so the spawn job is skipped instead of crashing the WorldThread.
 */
public class WorldSpawningSystemTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.spawning.world.system.WorldSpawningSystem";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        if (!EarlyConfigManager.getInstance().isTransformerEnabled("worldSpawningSystem")) {
            info("WorldSpawningSystemTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming WorldSpawningSystem...");
        verbose("Adding invalid ref guard in pickRandomChunk()");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new WorldSpawningSystemVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("WorldSpawningSystem transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform WorldSpawningSystem!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

package com.hyzenkernel.early;

import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - WorldConfigSaveSystem Transformer
 *
 * Ensures shared portal instances are persisted between restarts by forcing
 * DeleteOnUniverseStart/DeleteOnRemove to false before config saves.
 */
public class WorldConfigSaveSystemTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.server.core.universe.system.WorldConfigSaveSystem";

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public byte[] transform(String className, String packageName, byte[] classBytes) {
        if (!className.equals(TARGET_CLASS)) {
            return classBytes;
        }

        separator();
        info("Transforming WorldConfigSaveSystem...");
        verbose("Forcing shared portal instances to persist between restarts");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new WorldConfigSaveSystemVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("WorldConfigSaveSystem transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform WorldConfigSaveSystem!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

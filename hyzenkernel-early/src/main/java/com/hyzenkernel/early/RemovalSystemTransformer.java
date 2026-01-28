package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - RemovalSystem Transformer
 *
 * Prevents auto-removal of shared portal instances (instance-shared-*) so
 * portal devices keep a valid destination world.
 */
public class RemovalSystemTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.builtin.instances.removal.RemovalSystem";

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
            info("RemovalSystemTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming RemovalSystem...");
        verbose("Skipping auto-removal for instance-shared worlds");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new RemovalSystemVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("RemovalSystem transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform RemovalSystem!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

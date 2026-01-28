package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - InstancesPlugin Bytecode Transformer
 *
 * Rewrites spawnInstance(String, World, Transform) to reuse a shared instance
 * per instanceId (portal optimization).
 */
public class InstancesPluginTransformer implements ClassTransformer {

    private static final String TARGET_CLASS = "com.hypixel.hytale.builtin.instances.InstancesPlugin";

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
            info("InstancesPluginTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming InstancesPlugin...");
        verbose("Rewriting spawnInstance() to reuse shared portal instances");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new InstancesPluginVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("InstancesPlugin transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform InstancesPlugin!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

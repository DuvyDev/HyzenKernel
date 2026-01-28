package com.hyzenkernel.early;

import com.hyzenkernel.early.config.EarlyConfigManager;
import com.hypixel.hytale.plugin.early.ClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * HyzenKernel Early Plugin - PortalDeviceSummonPage Transformer
 *
 * Prevents stacking return portals by reusing the stored spawn point
 * for instance-shared worlds.
 */
public class PortalDeviceSummonPageTransformer implements ClassTransformer {

    private static final String TARGET_CLASS =
            "com.hypixel.hytale.builtin.portals.ui.PortalDeviceSummonPage";

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
            info("PortalDeviceSummonPageTransformer DISABLED by config");
            return classBytes;
        }

        separator();
        info("Transforming PortalDeviceSummonPage...");
        verbose("Skipping return portal spawn if shared instance already has spawn point");
        separator();

        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new PortalDeviceSummonPageVisitor(writer);

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            byte[] transformedBytes = writer.toByteArray();
            info("PortalDeviceSummonPage transformation COMPLETE!");
            verbose("Original size: " + classBytes.length + " bytes");
            verbose("Transformed size: " + transformedBytes.length + " bytes");

            return transformedBytes;
        } catch (Exception e) {
            error("ERROR: Failed to transform PortalDeviceSummonPage!");
            error("Returning original bytecode to prevent crash.", e);
            return classBytes;
        }
    }
}

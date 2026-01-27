package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzenkernel.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for PortalDeviceSummonPage transformation.
 * Replaces spawnReturnPortal with a stable-spawn implementation.
 */
public class PortalDeviceSummonVisitor extends ClassVisitor {

    private static final String TARGET_METHOD = "spawnReturnPortal";
    private static final String TARGET_DESC =
            "(Lcom/hypixel/hytale/server/core/universe/world/World;" +
            "Lcom/hypixel/hytale/builtin/portals/resources/PortalWorld;" +
            "Ljava/util/UUID;Ljava/lang/String;)" +
            "Ljava/util/concurrent/CompletableFuture;";

    public PortalDeviceSummonVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(TARGET_METHOD) && descriptor.equals(TARGET_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Replacing with PortalReturnHelper.spawnReturnPortal()");
            return new PortalDeviceSummonSpawnReturnMethodVisitor(mv);
        }

        return mv;
    }
}

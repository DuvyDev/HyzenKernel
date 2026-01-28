package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM ClassVisitor for PortalDeviceSummonPage transformation.
 */
public class PortalDeviceSummonPageVisitor extends ClassVisitor {

    private static final String TARGET_METHOD = "spawnReturnPortal";
    private static final String TARGET_DESC =
            "(Lcom/hypixel/hytale/server/core/universe/world/World;" +
            "Lcom/hypixel/hytale/builtin/portals/resources/PortalWorld;" +
            "Ljava/util/UUID;Ljava/lang/String;)" +
            "Ljava/util/concurrent/CompletableFuture;";

    public PortalDeviceSummonPageVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (TARGET_METHOD.equals(name) && TARGET_DESC.equals(descriptor)) {
            return new PortalDeviceSummonSpawnReturnMethodVisitor(mv);
        }

        return mv;
    }
}

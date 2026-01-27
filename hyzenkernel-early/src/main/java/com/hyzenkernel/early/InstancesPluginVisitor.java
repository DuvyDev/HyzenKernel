package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM ClassVisitor for InstancesPlugin transformation.
 */
public class InstancesPluginVisitor extends ClassVisitor {

    private static final String TARGET_METHOD = "spawnInstance";
    private static final String TARGET_DESC =
            "(Ljava/lang/String;Lcom/hypixel/hytale/server/core/universe/world/World;Lcom/hypixel/hytale/math/vector/Transform;)" +
            "Ljava/util/concurrent/CompletableFuture;";

    public InstancesPluginVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (TARGET_METHOD.equals(name) && TARGET_DESC.equals(descriptor)) {
            return new SpawnSharedInstanceMethodVisitor(mv);
        }

        return mv;
    }
}

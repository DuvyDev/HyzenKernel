package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM MethodVisitor that stabilizes PortalDeviceSummonPage.spawnReturnPortal(...)
 * by rewriting sampleUuid for shared instances (no external helper dependency).
 */
public class PortalDeviceSummonSpawnReturnMethodVisitor extends AdviceAdapter {

    public PortalDeviceSummonSpawnReturnMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor, Opcodes.ACC_STATIC, "spawnReturnPortal",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;" +
                        "Lcom/hypixel/hytale/builtin/portals/resources/PortalWorld;" +
                        "Ljava/util/UUID;Ljava/lang/String;)" +
                        "Ljava/util/concurrent/CompletableFuture;");
    }

    @Override
    protected void onMethodEnter() {
        Label skip = new Label();

        // if (world == null) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNULL, skip);

        // if (world.getName() == null) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        mv.visitJumpInsn(Opcodes.IFNULL, skip);

        // if (!world.getName().startsWith("instance-shared-")) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        mv.visitLdcInsn("instance-shared-");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false
        );
        mv.visitJumpInsn(Opcodes.IFEQ, skip);

        // sampleUuid = world.getWorldConfig().getUuid();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getWorldConfig",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "getUuid",
                "()Ljava/util/UUID;",
                false
        );
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitLabel(skip);
    }
}

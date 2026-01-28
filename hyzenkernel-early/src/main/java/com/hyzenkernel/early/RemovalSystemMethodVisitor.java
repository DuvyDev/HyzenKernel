package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM MethodVisitor that skips removal checks for instance-shared worlds.
 */
public class RemovalSystemMethodVisitor extends AdviceAdapter {

    protected RemovalSystemMethodVisitor(MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodExit(int opcode) {
        if (opcode != Opcodes.IRETURN) {
            return;
        }

        Label skip = new Label();

        int worldLocal = newLocal(Type.getType("Lcom/hypixel/hytale/server/core/universe/world/World;"));
        int nameLocal = newLocal(Type.getType("Ljava/lang/String;"));

        // World world = store.getExternalData().getWorld();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getExternalData",
                "()Ljava/lang/Object;",
                false
        );
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getWorld",
                "()Lcom/hypixel/hytale/server/core/universe/world/World;",
                false
        );
        mv.visitVarInsn(Opcodes.ASTORE, worldLocal);
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitJumpInsn(Opcodes.IFNULL, skip);

        // String name = world.getName();
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        mv.visitVarInsn(Opcodes.ASTORE, nameLocal);
        mv.visitVarInsn(Opcodes.ALOAD, nameLocal);
        mv.visitJumpInsn(Opcodes.IFNULL, skip);

        // if (name.startsWith("instance-shared-")) return false;
        mv.visitVarInsn(Opcodes.ALOAD, nameLocal);
        mv.visitLdcInsn("instance-shared-");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false
        );
        mv.visitJumpInsn(Opcodes.IFEQ, skip);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(skip);
    }
}

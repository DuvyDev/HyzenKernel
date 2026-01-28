package com.hyzenkernel.early;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM MethodVisitor that marks WorldConfig as changed after setSpawnProvider().
 */
public class WorldConfigSpawnProviderMethodVisitor extends AdviceAdapter {

    protected WorldConfigSpawnProviderMethodVisitor(MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodExit(int opcode) {
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "markChanged",
                "()V",
                false
        );
    }
}

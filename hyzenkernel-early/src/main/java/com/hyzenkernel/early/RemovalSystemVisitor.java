package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM ClassVisitor for RemovalSystem transformation.
 */
public class RemovalSystemVisitor extends ClassVisitor {

    private static final String TARGET_METHOD = "shouldRemoveWorld";
    private static final String TARGET_DESC = "(Lcom/hypixel/hytale/component/Store;)Z";

    public RemovalSystemVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (TARGET_METHOD.equals(name) && TARGET_DESC.equals(descriptor)) {
            return new RemovalSystemMethodVisitor(mv);
        }

        return mv;
    }
}

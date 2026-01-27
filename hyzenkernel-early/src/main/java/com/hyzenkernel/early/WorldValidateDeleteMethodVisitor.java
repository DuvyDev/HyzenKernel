package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that guards World.validateDeleteOnRemove()
 * to skip deleting shared portal instance worlds.
 */
public class WorldValidateDeleteMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;
    private final String className;

    public WorldValidateDeleteMethodVisitor(MethodVisitor methodVisitor, String className) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
        this.className = className;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label continueLabel = new Label();
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchLabel = new Label();
        Label end = new Label();

        target.visitCode();

        // if (this.name != null && this.name.startsWith("instance-shared-")) return;
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "name", "Ljava/lang/String;");
        target.visitJumpInsn(Opcodes.IFNULL, continueLabel);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "name", "Ljava/lang/String;");
        target.visitLdcInsn("instance-shared-");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        target.visitJumpInsn(Opcodes.IFEQ, continueLabel);
        target.visitInsn(Opcodes.RETURN);

        target.visitLabel(continueLabel);

        // if (!this.worldConfig.isDeleteOnRemove()) return;
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "worldConfig", "Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "isDeleteOnRemove",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, end);

        // try { FileUtil.deleteDirectory(this.getSavePath()); }
        target.visitLabel(tryStart);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getSavePath",
                "()Ljava/nio/file/Path;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/server/core/util/io/FileUtil",
                "deleteDirectory",
                "(Ljava/nio/file/Path;)V",
                false
        );
        target.visitLabel(tryEnd);
        target.visitJumpInsn(Opcodes.GOTO, end);

        // catch (Throwable t) { this.logger.at(Level.SEVERE).withCause(t).log("Exception while deleting world on remove:"); }
        target.visitLabel(catchLabel);
        target.visitVarInsn(Opcodes.ASTORE, 1);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitFieldInsn(Opcodes.GETFIELD, className, "logger", "Lcom/hypixel/hytale/logger/HytaleLogger;");
        target.visitFieldInsn(Opcodes.GETSTATIC, "java/util/logging/Level", "SEVERE", "Ljava/util/logging/Level;");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/logger/HytaleLogger",
                "at",
                "(Ljava/util/logging/Level;)Lcom/hypixel/hytale/logger/HytaleLogger$Api;",
                false
        );
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/hypixel/hytale/logger/HytaleLogger$Api",
                "withCause",
                "(Ljava/lang/Throwable;)Lcom/hypixel/hytale/logger/HytaleLogger$Api;",
                true
        );
        target.visitLdcInsn("Exception while deleting world on remove:");
        target.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/hypixel/hytale/logger/HytaleLogger$Api",
                "log",
                "(Ljava/lang/String;)V",
                true
        );

        target.visitLabel(end);
        target.visitInsn(Opcodes.RETURN);

        target.visitTryCatchBlock(tryStart, tryEnd, catchLabel, "java/lang/Throwable");
        target.visitMaxs(4, 2);
        target.visitEnd();
    }

    // Override all visit methods to ignore original bytecode
    @Override
    public void visitInsn(int opcode) {
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
    }

    @Override
    public void visitLabel(Label label) {
    }

    @Override
    public void visitLdcInsn(Object value) {
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String descriptor,
            org.objectweb.asm.Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
    }

    @Override
    public void visitEnd() {
    }

    @Override
    public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
    }

    @Override
    public void visitLineNumber(int line, Label start) {
    }

    @Override
    public void visitLocalVariable(String name, String descriptor, String signature,
            Label start, Label end, int index) {
    }
}

package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces RemovalSystem.shouldRemoveWorld(Store)
 * to skip removal for shared portal instance worlds.
 */
public class RemovalSystemMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;

    public RemovalSystemMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label notShared = new Label();
        Label sharedDone = new Label();
        Label configCheck = new Label();
        Label lengthCheck = new Label();
        Label loopStart = new Label();
        Label loopEnd = new Label();
        Label returnShouldRemove = new Label();

        target.visitCode();

        // World world = store.getExternalData().getWorld();
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "com/hypixel/hytale/component/Store", "getExternalData", "()Ljava/lang/Object;", false);
        target.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getWorld",
                "()Lcom/hypixel/hytale/server/core/universe/world/World;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 1);

        // String worldName = world.getName();
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 2);

        // boolean isShared = false;
        target.visitInsn(Opcodes.ICONST_0);
        target.visitVarInsn(Opcodes.ISTORE, 8);

        // if (worldName != null && worldName.startsWith("instance-shared-")) isShared = true;
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitJumpInsn(Opcodes.IFNULL, sharedDone);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitLdcInsn("instance-shared-");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        target.visitJumpInsn(Opcodes.IFEQ, sharedDone);
        target.visitInsn(Opcodes.ICONST_1);
        target.visitVarInsn(Opcodes.ISTORE, 8);
        target.visitLabel(sharedDone);

        // InstanceWorldConfig config = InstanceWorldConfig.get(world.getWorldConfig());
        target.visitLabel(notShared);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getWorldConfig",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/instances/config/InstanceWorldConfig",
                "get",
                "(Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;)Lcom/hypixel/hytale/builtin/instances/config/InstanceWorldConfig;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 3);

        // if (config == null) return false;
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitJumpInsn(Opcodes.IFNONNULL, configCheck);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitInsn(Opcodes.IRETURN);

        // RemovalCondition[] removalConditions = config.getRemovalConditions();
        target.visitLabel(configCheck);
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/config/InstanceWorldConfig",
                "getRemovalConditions",
                "()[Lcom/hypixel/hytale/builtin/instances/removal/RemovalCondition;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 4);

        // if (removalConditions.length == 0) return false;
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitInsn(Opcodes.ARRAYLENGTH);
        target.visitJumpInsn(Opcodes.IFNE, lengthCheck);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitInsn(Opcodes.IRETURN);

        // boolean shouldRemove = true;
        target.visitLabel(lengthCheck);
        target.visitInsn(Opcodes.ICONST_1);
        target.visitVarInsn(Opcodes.ISTORE, 5);

        // int i = 0;
        target.visitInsn(Opcodes.ICONST_0);
        target.visitVarInsn(Opcodes.ISTORE, 6);

        // loop start
        target.visitLabel(loopStart);
        target.visitVarInsn(Opcodes.ILOAD, 6);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitInsn(Opcodes.ARRAYLENGTH);
        target.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);

        // RemovalCondition cond = removalConditions[i];
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitVarInsn(Opcodes.ILOAD, 6);
        target.visitInsn(Opcodes.AALOAD);
        target.visitVarInsn(Opcodes.ASTORE, 7);

        // shouldRemove &= cond.shouldRemoveWorld(store);
        target.visitVarInsn(Opcodes.ILOAD, 5);
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/hypixel/hytale/builtin/instances/removal/RemovalCondition",
                "shouldRemoveWorld",
                "(Lcom/hypixel/hytale/component/Store;)Z",
                true
        );
        target.visitInsn(Opcodes.IAND);
        target.visitVarInsn(Opcodes.ISTORE, 5);

        // i++
        target.visitIincInsn(6, 1);
        target.visitJumpInsn(Opcodes.GOTO, loopStart);

        // return shouldRemove;
        target.visitLabel(loopEnd);
        target.visitVarInsn(Opcodes.ILOAD, 8);
        target.visitJumpInsn(Opcodes.IFEQ, returnShouldRemove);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitInsn(Opcodes.IRETURN);
        target.visitLabel(returnShouldRemove);
        target.visitVarInsn(Opcodes.ILOAD, 5);
        target.visitInsn(Opcodes.IRETURN);

        target.visitMaxs(4, 9);
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

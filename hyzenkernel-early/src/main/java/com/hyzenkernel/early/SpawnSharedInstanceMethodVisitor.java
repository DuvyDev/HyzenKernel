package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces InstancesPlugin.spawnInstance(String, World, Transform)
 * with a shared-instance implementation (portal reuse).
 */
public class SpawnSharedInstanceMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;

    public SpawnSharedInstanceMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        target.visitCode();

        Label fallback = new Label();
        Label shared = new Label();
        Label checkLoadable = new Label();
        Label spawnNew = new Label();

        // if (name == null) goto fallback
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitJumpInsn(Opcodes.IFNULL, fallback);

        // if (name.startsWith("Portals_")) goto shared
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitLdcInsn("Portals_");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        target.visitJumpInsn(Opcodes.IFNE, shared);

        // if (!name.startsWith("Portals")) goto fallback
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitLdcInsn("Portals");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        target.visitJumpInsn(Opcodes.IFEQ, fallback);

        // shared:
        target.visitLabel(shared);
        // String worldName = "instance-shared-" + InstancesPlugin.safeName(name);
        target.visitLdcInsn("instance-shared-");
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/instances/InstancesPlugin",
                "safeName",
                "(Ljava/lang/String;)Ljava/lang/String;",
                false
        );
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
        target.visitVarInsn(Opcodes.ASTORE, 4);

        // Universe universe = Universe.get();
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/server/core/universe/Universe",
                "get",
                "()Lcom/hypixel/hytale/server/core/universe/Universe;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 5);

        // World existing = universe.getWorld(worldName);
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/Universe",
                "getWorld",
                "(Ljava/lang/String;)Lcom/hypixel/hytale/server/core/universe/world/World;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 6);

        // if (existing != null && existing.isAlive()) return CompletableFuture.completedFuture(existing);
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitJumpInsn(Opcodes.IFNULL, checkLoadable);
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "isAlive",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, checkLoadable);
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "completedFuture",
                "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.ARETURN);

        // if (universe.isWorldLoadable(worldName)) return universe.loadWorld(worldName);
        target.visitLabel(checkLoadable);
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/Universe",
                "isWorldLoadable",
                "(Ljava/lang/String;)Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, spawnNew);
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/Universe",
                "loadWorld",
                "(Ljava/lang/String;)Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.ARETURN);

        // return this.spawnInstance(name, worldName, forWorld, returnPoint);
        target.visitLabel(spawnNew);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/InstancesPlugin",
                "spawnInstance",
                "(Ljava/lang/String;Ljava/lang/String;Lcom/hypixel/hytale/server/core/universe/world/World;Lcom/hypixel/hytale/math/vector/Transform;)" +
                        "Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.ARETURN);

        // fallback: return this.spawnInstance(name, null, forWorld, returnPoint);
        target.visitLabel(fallback);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitInsn(Opcodes.ACONST_NULL);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/InstancesPlugin",
                "spawnInstance",
                "(Ljava/lang/String;Ljava/lang/String;Lcom/hypixel/hytale/server/core/universe/world/World;Lcom/hypixel/hytale/math/vector/Transform;)" +
                        "Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.ARETURN);

        target.visitMaxs(6, 7);
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

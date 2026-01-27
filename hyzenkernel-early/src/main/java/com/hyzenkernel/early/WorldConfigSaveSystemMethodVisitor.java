package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces WorldConfigSaveSystem.saveWorldConfigAndResources(World)
 * to force shared portal instance configs to persist between restarts.
 */
public class WorldConfigSaveSystemMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;

    public WorldConfigSaveSystemMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label skipShared = new Label();
        Label saveCheck = new Label();
        Label saveElse = new Label();
        Label returnLabel = new Label();

        target.visitCode();

        // WorldConfig worldConfig = world.getWorldConfig();
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getWorldConfig",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 1);

        // String worldName = world.getName();
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 2);

        // if (worldName == null || !worldName.startsWith("instance-shared-")) goto skipShared;
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitJumpInsn(Opcodes.IFNULL, skipShared);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitLdcInsn("instance-shared-");
        target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "startsWith", "(Ljava/lang/String;)Z", false);
        target.visitJumpInsn(Opcodes.IFEQ, skipShared);

        // if (worldConfig.isDeleteOnUniverseStart()) worldConfig.setDeleteOnUniverseStart(false);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "isDeleteOnUniverseStart",
                "()Z",
                false
        );
        Label skipDeleteOnStart = new Label();
        target.visitJumpInsn(Opcodes.IFEQ, skipDeleteOnStart);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "setDeleteOnUniverseStart",
                "(Z)V",
                false
        );
        target.visitLabel(skipDeleteOnStart);

        // if (worldConfig.isDeleteOnRemove()) worldConfig.setDeleteOnRemove(false);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "isDeleteOnRemove",
                "()Z",
                false
        );
        Label skipDeleteOnRemove = new Label();
        target.visitJumpInsn(Opcodes.IFEQ, skipDeleteOnRemove);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "setDeleteOnRemove",
                "(Z)V",
                false
        );
        target.visitLabel(skipDeleteOnRemove);

        // worldConfig.markChanged();
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "markChanged",
                "()V",
                false
        );

        target.visitLabel(skipShared);

        // if (worldConfig.isSavingConfig() && worldConfig.consumeHasChanged()) { ... }
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "isSavingConfig",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, saveElse);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "consumeHasChanged",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, saveElse);

        // CompletableFuture.allOf(
        //   world.getChunkStore().getStore().saveAllResources(),
        //   world.getEntityStore().getStore().saveAllResources(),
        //   Universe.get().getWorldConfigProvider().save(world.getSavePath(), world.getWorldConfig(), world)
        // )
        target.visitInsn(Opcodes.ICONST_3);
        target.visitTypeInsn(Opcodes.ANEWARRAY, "java/util/concurrent/CompletableFuture");
        target.visitInsn(Opcodes.DUP);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getChunkStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/ChunkStore;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "saveAllResources",
                "()Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.AASTORE);

        target.visitInsn(Opcodes.DUP);
        target.visitInsn(Opcodes.ICONST_1);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getEntityStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/EntityStore;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/EntityStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "saveAllResources",
                "()Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.AASTORE);

        target.visitInsn(Opcodes.DUP);
        target.visitInsn(Opcodes.ICONST_2);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/server/core/universe/Universe",
                "get",
                "()Lcom/hypixel/hytale/server/core/universe/Universe;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/Universe",
                "getWorldConfigProvider",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfigProvider;",
                false
        );
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getSavePath",
                "()Ljava/nio/file/Path;",
                false
        );
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getWorldConfig",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;",
                false
        );
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/hypixel/hytale/server/core/universe/world/WorldConfigProvider",
                "save",
                "(Ljava/nio/file/Path;Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;Lcom/hypixel/hytale/server/core/universe/world/World;)Ljava/util/concurrent/CompletableFuture;",
                true
        );
        target.visitInsn(Opcodes.AASTORE);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "allOf",
                "([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitJumpInsn(Opcodes.GOTO, returnLabel);

        // else: CompletableFuture.allOf(chunkStore.saveAllResources(), entityStore.saveAllResources())
        target.visitLabel(saveElse);
        target.visitInsn(Opcodes.ICONST_2);
        target.visitTypeInsn(Opcodes.ANEWARRAY, "java/util/concurrent/CompletableFuture");
        target.visitInsn(Opcodes.DUP);
        target.visitInsn(Opcodes.ICONST_0);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getChunkStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/ChunkStore;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "saveAllResources",
                "()Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.AASTORE);

        target.visitInsn(Opcodes.DUP);
        target.visitInsn(Opcodes.ICONST_1);
        target.visitVarInsn(Opcodes.ALOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getEntityStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/EntityStore;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/EntityStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "saveAllResources",
                "()Ljava/util/concurrent/CompletableFuture;",
                false
        );
        target.visitInsn(Opcodes.AASTORE);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "allOf",
                "([Ljava/util/concurrent/CompletableFuture;)Ljava/util/concurrent/CompletableFuture;",
                false
        );

        target.visitLabel(returnLabel);
        target.visitInsn(Opcodes.ARETURN);

        target.visitMaxs(6, 3);
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

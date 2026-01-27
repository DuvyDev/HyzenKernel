package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that replaces ChunkSavingSystems.tryQueue(...)
 * to skip saving already-on-disk chunks for shared instances.
 */
public class ChunkSavingTryQueueMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;

    public ChunkSavingTryQueueMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label returnLabel = new Label();
        Label doSave = new Label();

        target.visitCode();

        // WorldChunk worldChunkComponent = archetypeChunk.getComponent(index, WORLD_CHUNK_COMPONENT_TYPE);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitVarInsn(Opcodes.ILOAD, 0);
        target.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/hypixel/hytale/server/core/universe/world/storage/component/ChunkSavingSystems",
                "WORLD_CHUNK_COMPONENT_TYPE",
                "Lcom/hypixel/hytale/component/ComponentType;"
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/ArchetypeChunk",
                "getComponent",
                "(ILcom/hypixel/hytale/component/ComponentType;)Lcom/hypixel/hytale/component/Component;",
                false
        );
        target.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk");
        target.visitVarInsn(Opcodes.ASTORE, 3);

        // if (!worldChunkComponent.getNeedsSaving()) return;
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk",
                "getNeedsSaving",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, returnLabel);

        // if (worldChunkComponent.isSaving()) return;
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk",
                "isSaving",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFNE, returnLabel);

        // if (worldChunkComponent.is(ChunkFlag.ON_DISK)) { ... }
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/hypixel/hytale/server/core/universe/world/chunk/ChunkFlag",
                "ON_DISK",
                "Lcom/hypixel/hytale/server/core/universe/world/chunk/ChunkFlag;"
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk",
                "is",
                "(Lcom/hypixel/hytale/server/core/universe/world/chunk/ChunkFlag;)Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, doSave);

        // World world = store.getExternalData().getWorld();
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getExternalData",
                "()Ljava/lang/Object;",
                false
        );
        target.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getWorld",
                "()Lcom/hypixel/hytale/server/core/universe/world/World;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 4);

        // if (world == null) goto doSave
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitJumpInsn(Opcodes.IFNULL, doSave);

        // String worldName = world.getName();
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 5);

        // if (worldName == null) goto doSave
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitJumpInsn(Opcodes.IFNULL, doSave);

        // if (!worldName.startsWith("instance-shared-")) goto doSave
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitLdcInsn("instance-shared-");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, doSave);

        // worldChunkComponent.consumeNeedsSaving(); return;
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/chunk/WorldChunk",
                "consumeNeedsSaving",
                "()Z",
                false
        );
        target.visitInsn(Opcodes.POP);
        target.visitJumpInsn(Opcodes.GOTO, returnLabel);

        // Original save logic
        target.visitLabel(doSave);
        // Ref<ChunkStore> chunkRef = archetypeChunk.getReferenceTo(index);
        target.visitVarInsn(Opcodes.ALOAD, 1);
        target.visitVarInsn(Opcodes.ILOAD, 0);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/ArchetypeChunk",
                "getReferenceTo",
                "(I)Lcom/hypixel/hytale/component/Ref;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 6);

        // ChunkSaveEvent event = new ChunkSaveEvent(worldChunkComponent);
        target.visitTypeInsn(Opcodes.NEW, "com/hypixel/hytale/server/core/universe/world/events/ecs/ChunkSaveEvent");
        target.visitInsn(Opcodes.DUP);
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "com/hypixel/hytale/server/core/universe/world/events/ecs/ChunkSaveEvent",
                "<init>",
                "(Lcom/hypixel/hytale/server/core/universe/world/chunk/WorldChunk;)V",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 7);

        // store.invoke(chunkRef, event);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "invoke",
                "(Lcom/hypixel/hytale/component/Ref;Lcom/hypixel/hytale/component/system/EcsEvent;)V",
                false
        );

        // if (!event.isCancelled()) store.getResource(ChunkStore.SAVE_RESOURCE).push(chunkRef);
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/system/CancellableEcsEvent",
                "isCancelled",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFNE, returnLabel);
        target.visitVarInsn(Opcodes.ALOAD, 2);
        target.visitFieldInsn(
                Opcodes.GETSTATIC,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "SAVE_RESOURCE",
                "Lcom/hypixel/hytale/component/ResourceType;"
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getResource",
                "(Lcom/hypixel/hytale/component/ResourceType;)Lcom/hypixel/hytale/component/Resource;",
                false
        );
        target.visitTypeInsn(
                Opcodes.CHECKCAST,
                "com/hypixel/hytale/server/core/universe/world/storage/component/ChunkSavingSystems$Data"
        );
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/component/ChunkSavingSystems$Data",
                "push",
                "(Lcom/hypixel/hytale/component/Ref;)V",
                false
        );

        target.visitLabel(returnLabel);
        target.visitInsn(Opcodes.RETURN);

        target.visitMaxs(6, 8);
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

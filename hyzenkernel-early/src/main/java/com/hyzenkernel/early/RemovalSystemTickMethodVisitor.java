package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that rewrites RemovalSystem.tick(...)
 * to keep instance-shared worlds alive while still updating timers.
 */
public class RemovalSystemTickMethodVisitor extends MethodVisitor {

    private final MethodVisitor target;

    public RemovalSystemTickMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, null);
        this.target = methodVisitor;
    }

    @Override
    public void visitCode() {
        generateFixedMethod();
    }

    private void generateFixedMethod() {
        Label returnLabel = new Label();
        Label checkRemove = new Label();

        target.visitCode();

        // InstanceDataResource data = store.getResource(InstanceDataResource.getResourceType());
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "getResourceType",
                "()Lcom/hypixel/hytale/component/ResourceType;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getResource",
                "(Lcom/hypixel/hytale/component/ResourceType;)Lcom/hypixel/hytale/component/Resource;",
                false
        );
        target.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource");
        target.visitVarInsn(Opcodes.ASTORE, 4);

        // if (data.isRemoving()) return;
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "isRemoving",
                "()Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFNE, returnLabel);

        // if (!RemovalSystem.shouldRemoveWorld(store)) return;
        target.visitVarInsn(Opcodes.ALOAD, 3);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/instances/removal/RemovalSystem",
                "shouldRemoveWorld",
                "(Lcom/hypixel/hytale/component/Store;)Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, returnLabel);

        // World world = ((ChunkStore) store.getExternalData()).getWorld();
        target.visitVarInsn(Opcodes.ALOAD, 3);
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
        target.visitVarInsn(Opcodes.ASTORE, 5);

        // if (world == null) return;
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitJumpInsn(Opcodes.IFNULL, returnLabel);

        // String name = world.getName();
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        target.visitVarInsn(Opcodes.ASTORE, 6);

        // if (name == null) return;
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitJumpInsn(Opcodes.IFNULL, returnLabel);

        // if (name.startsWith("instance-shared-")) handle timeout-only removal
        Label notShared = new Label();
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitLdcInsn("instance-shared-");
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false
        );
        target.visitJumpInsn(Opcodes.IFEQ, notShared);

        // PortalWorld portalWorld = world.getEntityStore().getStore().getResource(PortalWorld.getResourceType());
        target.visitVarInsn(Opcodes.ALOAD, 5);
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
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getResourceType",
                "()Lcom/hypixel/hytale/component/ResourceType;",
                false
        );
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getResource",
                "(Lcom/hypixel/hytale/component/ResourceType;)Lcom/hypixel/hytale/component/Resource;",
                false
        );
        target.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/builtin/portals/resources/PortalWorld");
        target.visitVarInsn(Opcodes.ASTORE, 7);

        // if (portalWorld == null) return;
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitJumpInsn(Opcodes.IFNULL, returnLabel);

        // if (portalWorld.getPortalType() == null) return;
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getPortalType",
                "()Lcom/hypixel/hytale/server/core/asset/type/portalworld/PortalType;",
                false
        );
        target.visitJumpInsn(Opcodes.IFNULL, returnLabel);

        // if (portalWorld.getRemainingSeconds(world) > 0) return;
        target.visitVarInsn(Opcodes.ALOAD, 7);
        target.visitVarInsn(Opcodes.ALOAD, 5);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getRemainingSeconds",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;)D",
                false
        );
        target.visitInsn(Opcodes.DCONST_0);
        target.visitInsn(Opcodes.DCMPL);
        target.visitJumpInsn(Opcodes.IFGT, returnLabel);

        target.visitLabel(notShared);

        // data.setRemoving(true);
        target.visitVarInsn(Opcodes.ALOAD, 4);
        target.visitInsn(Opcodes.ICONST_1);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setRemoving",
                "(Z)V",
                false
        );

        // Universe.get().removeWorld(name);
        target.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/server/core/universe/Universe",
                "get",
                "()Lcom/hypixel/hytale/server/core/universe/Universe;",
                false
        );
        target.visitVarInsn(Opcodes.ALOAD, 6);
        target.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/Universe",
                "removeWorld",
                "(Ljava/lang/String;)Z",
                false
        );
        target.visitInsn(Opcodes.POP);

        target.visitLabel(returnLabel);
        target.visitInsn(Opcodes.RETURN);
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

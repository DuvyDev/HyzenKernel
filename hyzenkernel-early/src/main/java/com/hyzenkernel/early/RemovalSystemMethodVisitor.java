package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM MethodVisitor that short-circuits removal checks for instance-shared worlds
 * until their portal timer reaches 0.
 */
public class RemovalSystemMethodVisitor extends AdviceAdapter {

    protected RemovalSystemMethodVisitor(MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(Opcodes.ASM9, methodVisitor, access, name, descriptor);
    }

    @Override
    protected void onMethodEnter() {
        Label skip = new Label();
        Label portalWorldOk = new Label();
        Label portalTypeOk = new Label();

        int worldLocal = newLocal(Type.getType("Lcom/hypixel/hytale/server/core/universe/world/World;"));
        int nameLocal = newLocal(Type.getType("Ljava/lang/String;"));
        int portalWorldLocal = newLocal(Type.getType("Lcom/hypixel/hytale/builtin/portals/resources/PortalWorld;"));
        int instanceDataLocal = newLocal(Type.getType("Lcom/hypixel/hytale/builtin/instances/removal/InstanceDataResource;"));
        int secondsLocal = newLocal(Type.DOUBLE_TYPE);

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

        // if (!name.startsWith("instance-shared-")) goto skip
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

        // PortalWorld portalWorld = world.getEntityStore().getStore().getResource(PortalWorld.getResourceType());
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getEntityStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/EntityStore;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/EntityStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getResourceType",
                "()Lcom/hypixel/hytale/component/ResourceType;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getResource",
                "(Lcom/hypixel/hytale/component/ResourceType;)Lcom/hypixel/hytale/component/Resource;",
                false
        );
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/builtin/portals/resources/PortalWorld");
        mv.visitVarInsn(Opcodes.ASTORE, portalWorldLocal);

        // if (portalWorld == null) return false;
        mv.visitVarInsn(Opcodes.ALOAD, portalWorldLocal);
        mv.visitJumpInsn(Opcodes.IFNONNULL, portalWorldOk);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(portalWorldOk);

        // if (portalWorld.getPortalType() == null) return false;
        mv.visitVarInsn(Opcodes.ALOAD, portalWorldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getPortalType",
                "()Lcom/hypixel/hytale/server/core/asset/type/portalworld/PortalType;",
                false
        );
        mv.visitJumpInsn(Opcodes.IFNONNULL, portalTypeOk);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitLabel(portalTypeOk);

        // InstanceDataResource instanceData = world.getChunkStore().getStore().getResource(InstanceDataResource.getResourceType());
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getChunkStore",
                "()Lcom/hypixel/hytale/server/core/universe/world/storage/ChunkStore;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/storage/ChunkStore",
                "getStore",
                "()Lcom/hypixel/hytale/component/Store;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "getResourceType",
                "()Lcom/hypixel/hytale/component/ResourceType;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/component/Store",
                "getResource",
                "(Lcom/hypixel/hytale/component/ResourceType;)Lcom/hypixel/hytale/component/Resource;",
                false
        );
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource");
        mv.visitVarInsn(Opcodes.ASTORE, instanceDataLocal);

        // if (instanceData.getTimeoutTimer() == null) portalWorld.setRemainingSeconds(world, portalWorld.getRemainingSeconds(world));
        Label timerSetSkip = new Label();
        mv.visitVarInsn(Opcodes.ALOAD, instanceDataLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "getTimeoutTimer",
                "()Ljava/time/Instant;",
                false
        );
        mv.visitJumpInsn(Opcodes.IFNONNULL, timerSetSkip);

        mv.visitVarInsn(Opcodes.ALOAD, portalWorldLocal);
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getRemainingSeconds",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;)D",
                false
        );
        mv.visitVarInsn(Opcodes.DSTORE, secondsLocal);

        mv.visitVarInsn(Opcodes.ALOAD, portalWorldLocal);
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitVarInsn(Opcodes.DLOAD, secondsLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "setRemainingSeconds",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;D)V",
                false
        );

        mv.visitLabel(timerSetSkip);

        // if (portalWorld.getRemainingSeconds(world) > 0) return false;
        mv.visitVarInsn(Opcodes.ALOAD, portalWorldLocal);
        mv.visitVarInsn(Opcodes.ALOAD, worldLocal);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getRemainingSeconds",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;)D",
                false
        );
        mv.visitInsn(Opcodes.DCONST_0);
        mv.visitInsn(Opcodes.DCMPL);
        mv.visitJumpInsn(Opcodes.IFLE, skip);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitInsn(Opcodes.IRETURN);

        mv.visitLabel(skip);
    }
}

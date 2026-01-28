package com.hyzenkernel.early;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * ASM MethodVisitor that prevents stacking return portals for shared instances.
 */
public class PortalDeviceSummonSpawnReturnMethodVisitor extends AdviceAdapter {

    public PortalDeviceSummonSpawnReturnMethodVisitor(MethodVisitor methodVisitor) {
        super(Opcodes.ASM9, methodVisitor, Opcodes.ACC_STATIC, "spawnReturnPortal",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;" +
                        "Lcom/hypixel/hytale/builtin/portals/resources/PortalWorld;" +
                        "Ljava/util/UUID;Ljava/lang/String;)" +
                        "Ljava/util/concurrent/CompletableFuture;");
    }

    @Override
    protected void onMethodEnter() {
        Label continueOriginal = new Label();
        Label spawnPointMissing = new Label();
        Label noSpawnProvider = new Label();
        Label noSpawnTransform = new Label();

        // if (world == null) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitJumpInsn(Opcodes.IFNULL, continueOriginal);

        // if (world.getName() == null) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        mv.visitJumpInsn(Opcodes.IFNULL, continueOriginal);

        // if (!world.getName().startsWith("instance-shared-")) skip
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getName",
                "()Ljava/lang/String;",
                false
        );
        mv.visitLdcInsn("instance-shared-");
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "java/lang/String",
                "startsWith",
                "(Ljava/lang/String;)Z",
                false
        );
        mv.visitJumpInsn(Opcodes.IFEQ, continueOriginal);

        // if (portalWorld == null) skip
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitJumpInsn(Opcodes.IFNULL, continueOriginal);

        // Reset timer for shared instances on every portal open
        // portalWorld.setRemainingSeconds(world, portalWorld.getTimeLimitSeconds());
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getTimeLimitSeconds",
                "()I",
                false
        );
        mv.visitInsn(Opcodes.I2D);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "setRemainingSeconds",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;D)V",
                false
        );

        // Reset InstanceDataResource timers so shared worlds don't instantly close
        int dataLocal = newLocal(Type.getType("Lcom/hypixel/hytale/builtin/instances/removal/InstanceDataResource;"));
        mv.visitVarInsn(Opcodes.ALOAD, 0);
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
        mv.visitVarInsn(Opcodes.ASTORE, dataLocal);

        mv.visitVarInsn(Opcodes.ALOAD, dataLocal);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setTimeoutTimer",
                "(Ljava/time/Instant;)V",
                false
        );
        mv.visitVarInsn(Opcodes.ALOAD, dataLocal);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setIdleTimeoutTimer",
                "(Ljava/time/Instant;)V",
                false
        );
        mv.visitVarInsn(Opcodes.ALOAD, dataLocal);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setWorldTimeoutTimer",
                "(Ljava/time/Instant;)V",
                false
        );
        mv.visitVarInsn(Opcodes.ALOAD, dataLocal);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setHadPlayer",
                "(Z)V",
                false
        );
        mv.visitVarInsn(Opcodes.ALOAD, dataLocal);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/instances/removal/InstanceDataResource",
                "setRemoving",
                "(Z)V",
                false
        );

        // If a spawn point already exists, skip spawning a new return portal
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "getSpawnPoint",
                "()Lcom/hypixel/hytale/math/vector/Transform;",
                false
        );
        mv.visitJumpInsn(Opcodes.IFNULL, spawnPointMissing);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "completedFuture",
                "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;",
                false
        );
        mv.visitInsn(Opcodes.ARETURN);

        // If spawnPoint is missing but spawnProvider is IndividualSpawnProvider,
        // reuse that spawn point and skip spawning a new portal.
        mv.visitLabel(spawnPointMissing);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/World",
                "getWorldConfig",
                "()Lcom/hypixel/hytale/server/core/universe/world/WorldConfig;",
                false
        );
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/server/core/universe/world/WorldConfig",
                "getSpawnProvider",
                "()Lcom/hypixel/hytale/server/core/universe/world/spawn/ISpawnProvider;",
                false
        );
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNULL, noSpawnProvider);
        mv.visitInsn(Opcodes.DUP);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, "com/hypixel/hytale/server/core/universe/world/spawn/IndividualSpawnProvider");
        mv.visitJumpInsn(Opcodes.IFEQ, noSpawnProvider);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(
                Opcodes.INVOKEINTERFACE,
                "com/hypixel/hytale/server/core/universe/world/spawn/ISpawnProvider",
                "getSpawnPoint",
                "(Lcom/hypixel/hytale/server/core/universe/world/World;Ljava/util/UUID;)" +
                        "Lcom/hypixel/hytale/math/vector/Transform;",
                true
        );
        mv.visitInsn(Opcodes.DUP);
        mv.visitJumpInsn(Opcodes.IFNULL, noSpawnTransform);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitInsn(Opcodes.SWAP);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/hypixel/hytale/builtin/portals/resources/PortalWorld",
                "setSpawnPoint",
                "(Lcom/hypixel/hytale/math/vector/Transform;)V",
                false
        );
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "java/util/concurrent/CompletableFuture",
                "completedFuture",
                "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;",
                false
        );
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitLabel(noSpawnTransform);
        mv.visitInsn(Opcodes.POP);
        mv.visitJumpInsn(Opcodes.GOTO, continueOriginal);

        mv.visitLabel(noSpawnProvider);
        mv.visitInsn(Opcodes.POP);
        mv.visitJumpInsn(Opcodes.GOTO, continueOriginal);

        mv.visitLabel(continueOriginal);
        // fall through to original method to spawn a new portal
    }
}

package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzenkernel.early.EarlyLogger.verbose;

/**
 * ASM ClassVisitor for ChunkSavingSystems transformation.
 * Rewrites tryQueue and tryQueueSync to skip saving already-on-disk
 * chunks for shared portal instances.
 */
public class ChunkSavingSystemsVisitor extends ClassVisitor {

    private static final String TRY_QUEUE = "tryQueue";
    private static final String TRY_QUEUE_DESC =
            "(ILcom/hypixel/hytale/component/ArchetypeChunk;Lcom/hypixel/hytale/component/Store;)V";

    private static final String TRY_QUEUE_SYNC = "tryQueueSync";
    private static final String TRY_QUEUE_SYNC_DESC =
            "(Lcom/hypixel/hytale/component/ArchetypeChunk;Lcom/hypixel/hytale/component/CommandBuffer;)V";

    public ChunkSavingSystemsVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(TRY_QUEUE) && descriptor.equals(TRY_QUEUE_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying shared-instance static chunk save filter (tryQueue)...");
            return new ChunkSavingTryQueueMethodVisitor(mv);
        }

        if (name.equals(TRY_QUEUE_SYNC) && descriptor.equals(TRY_QUEUE_SYNC_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying shared-instance static chunk save filter (tryQueueSync)...");
            return new ChunkSavingTryQueueSyncMethodVisitor(mv);
        }

        return mv;
    }
}

package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * ASM ClassVisitor for InteractionChain.
 *
 * This visitor intercepts problematic methods and applies fixes:
 * 1. putInteractionSyncData - buffer overflow when data arrives out of order
 * 2. updateSyncPosition - throws IllegalArgumentException on sync gaps
 */
public class InteractionChainVisitor extends ClassVisitor {

    private static final String PUT_SYNC_DATA_METHOD = "putInteractionSyncData";
    private static final String UPDATE_SYNC_POSITION_METHOD = "updateSyncPosition";
    private static final String REMOVE_INTERACTION_ENTRY_METHOD = "removeInteractionEntry";

    private String className;

    public InteractionChainVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals(PUT_SYNC_DATA_METHOD)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying buffer overflow fix...");
            return new PutSyncDataMethodVisitor(mv, className);
        }

        if (name.equals(UPDATE_SYNC_POSITION_METHOD)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying sync position fix...");
            return new UpdateSyncPositionMethodVisitor(mv, className);
        }

        if (name.equals(REMOVE_INTERACTION_ENTRY_METHOD)) {
            // NOTE: Newer server versions no longer throw here (they call flagDesync instead),
            // and the old try/catch patch can break verification. Skip this fix for stability.
            verbose("Found method: " + name + descriptor);
            verbose("Skipping removeInteractionEntry patch (Issue #40) on modern server builds");
            return mv;
        }

        return mv;
    }
}

package com.hyzenkernel.early;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.hyzenkernel.early.EarlyLogger.*;

/**
 * ASM ClassVisitor for World class transformation.
 * Intercepts the addPlayer method to apply the race condition fix.
 */
public class WorldVisitor extends ClassVisitor {

    private String className;

    // Method we're targeting - the 4-parameter version that does the actual work
    private static final String ADD_PLAYER_METHOD = "addPlayer";
    private static final String ADD_PLAYER_DESC = "(Lcom/hypixel/hytale/server/core/universe/PlayerRef;Lcom/hypixel/hytale/math/vector/Transform;Ljava/lang/Boolean;Ljava/lang/Boolean;)Ljava/util/concurrent/CompletableFuture;";
    private static final String EXECUTE_METHOD = "execute";
    private static final String EXECUTE_DESC = "(Ljava/lang/Runnable;)V";
    private static final String VALIDATE_DELETE_METHOD = "validateDeleteOnRemove";
    private static final String VALIDATE_DELETE_DESC = "()V";

    public WorldVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // Only transform the 4-parameter addPlayer method
        if (name.equals(ADD_PLAYER_METHOD) && descriptor.equals(ADD_PLAYER_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying instance teleport race condition fix...");
            return new WorldAddPlayerMethodVisitor(mv, className);
        }

        if (name.equals(EXECUTE_METHOD) && descriptor.equals(EXECUTE_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying World.execute shutdown guard...");
            return new WorldExecuteMethodVisitor(mv, className);
        }

        if (name.equals(VALIDATE_DELETE_METHOD) && descriptor.equals(VALIDATE_DELETE_DESC)) {
            verbose("Found method: " + name + descriptor);
            verbose("Applying shared instance delete guard...");
            return new WorldValidateDeleteMethodVisitor(mv, className);
        }

        return mv;
    }
}

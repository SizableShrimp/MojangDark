/*
 * Copyright (c) 2021 SizableShrimp
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.sizableshrimp.mojangdark.transform;

import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

public class MojangDarkLaunchPlugin {
    private interface NodeConsumer {
        boolean accept(ClassNode classNode) throws Exception;
    }

    public static final int LOGO_BACKGROUND_COLOR = packColor(255, 239, 50, 61);
    public static final int LOGO_BACKGROUND_COLOR_DARK = packColor(255, 0, 0, 0);
    public static final IntSupplier BRAND_BACKGROUND_SUPPLIER = () -> MojangDarkBootstrap.isDark() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;
    private static final Map<String, float[]> LIGHT_TO_DARK = ImmutableMap.of(
            "MC", new float[]{1.0F, 1.0F, 1.0F}, // MC loader
            "ML", new float[]{0.0F, 0.5F, 1.0F}, // Mod Loader
            "LOC", new float[]{0.0F, 0.5F, 0.0F}, // LOCator
            "MOD", new float[]{1.0F, 0.0F, 0.0F} // MOD - Forge's version color for some reason?
    );

    private static final Logger LOGGER = LogManager.getLogger();
    public static final Map<String, NodeConsumer> CLASSES = ImmutableMap.<String, NodeConsumer>builder()
            .put("net/minecraftforge/fml/loading/progress/ClientVisualization.class", classNode -> {
                MethodNode methodNode = findMethodNode(classNode, "renderBackground", "()V");
                MethodInsnNode instruction = findInstructionNode(methodNode, Opcodes.INVOKESTATIC, insn ->
                        "org/lwjgl/opengl/GL11".equals(insn.owner) && "glColor4f".equals(insn.name) && "(FFFF)V".equals(insn.desc));
                methodNode.instructions.insertBefore(instruction, getInvokeStaticInsn("drawBackgroundColor", float.class, float.class, float.class, float.class));
                methodNode.instructions.remove(instruction);
                return true;
            })
            .put("net/minecraftforge/fml/loading/progress/StartupMessageManager$MessageType.class", classNode -> {
                MethodNode methodNode = findMethodNode(classNode, "colour", "()[F");
                // ALOAD 0, INVOKEVIRTUAL Enum.name(), ALOAD 0, GETFIELD, INVOKESTATIC convertColor, ARETURN - done
                FieldInsnNode instruction = findInstructionNode(methodNode, Opcodes.GETFIELD, insn -> "colour".equals(insn.name));
                methodNode.instructions.insertBefore(instruction, new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "net/minecraftforge/fml/loading/progress/StartupMessageManager$MessageType", "name", "()Ljava/lang/String;"));
                methodNode.instructions.insertBefore(instruction, new VarInsnNode(Opcodes.ALOAD, 0));
                // GETFIELD is here
                methodNode.instructions.insert(instruction, getInvokeStaticInsn("convertColor", String.class, float[].class));
                return true;
            })
            .build();

    public static byte[] transform(String name, byte[] input) {
        ClassNode classNode = new ClassNode();
        new ClassReader(input).accept(classNode, ClassReader.EXPAND_FRAMES);

        boolean modified = false;
        try {
            modified = CLASSES.get(name).accept(classNode);
        } catch (Exception e) {
            LOGGER.error("Error when transforming", e);
        }

        if (modified) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);
            return cw.toByteArray();
        }
        return input;
    }

    private static MethodInsnNode getInvokeStaticInsn(String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        String internalName = Type.getType(MojangDarkLaunchPlugin.class).getInternalName();
        Method method = MojangDarkLaunchPlugin.class.getMethod(methodName, parameterTypes);
        Type methodType = Type.getType(method);

        return new MethodInsnNode(Opcodes.INVOKESTATIC, internalName, method.getName(), methodType.getDescriptor());
    }

    private static MethodNode findMethodNode(ClassNode classNode, String methodName, String descriptor) throws Exception {
        for (MethodNode methodNode : classNode.methods) {
            if (Objects.equals(methodNode.name, methodName) && Objects.equals(methodNode.desc, descriptor)) {
                return methodNode;
            }
        }
        throw new Exception(String.format("No method node found with name %s and descriptor %s", methodName, descriptor));
    }

    private static <T extends AbstractInsnNode> T findInstructionNode(MethodNode methodNode, int opcode, Predicate<T> applier) throws Exception {
        for (AbstractInsnNode instruction : methodNode.instructions) {
            if (instruction.getOpcode() == opcode) {
                @SuppressWarnings("unchecked")
                T castedInsn = (T) instruction;
                boolean success = applier.test(castedInsn);
                if (success)
                    return castedInsn;
            }
        }
        throw new Exception(String.format("No instruction node found in method %s and opcode %d", methodNode.name, opcode));
    }

    // Public for coremod
    public static float[] convertColor(String name, float[] color) {
        return MojangDarkBootstrap.isDark() ? LIGHT_TO_DARK.get(name) : color;
    }

    // Have to accept these floats so that the FLOADs don't need to be removed
    // Public for coremod
    public static void drawBackgroundColor(float red, float green, float blue, float alpha) {
        int packedColor = BRAND_BACKGROUND_SUPPLIER.getAsInt();
        red = red(packedColor) / 255.0F;
        green = green(packedColor) / 255.0F;
        blue = blue(packedColor) / 255.0F;
        alpha = alpha(packedColor) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    private static int packColor(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int alpha(int packedColor) {
        return packedColor >>> 24;
    }

    private static int red(int packedColor) {
        return packedColor >> 16 & 255;
    }

    private static int green(int packedColor) {
        return packedColor >> 8 & 255;
    }

    private static int blue(int packedColor) {
        return packedColor & 255;
    }
}

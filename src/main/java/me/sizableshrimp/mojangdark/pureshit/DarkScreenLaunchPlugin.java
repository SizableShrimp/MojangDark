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

package me.sizableshrimp.mojangdark.pureshit;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.coremod.api.ASMAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Can't reference ANYTHING except this class because classloading issues
public class DarkScreenLaunchPlugin {
    private interface NodeConsumer {
        boolean accept(ClassNode classNode) throws Exception;
    }

    public static final int LOGO_BACKGROUND_COLOR = packColor(255, 239, 50, 61);
    public static final int LOGO_BACKGROUND_COLOR_DARK = packColor(255, 0, 0, 0);
    private static final IntSupplier BRAND_BACKGROUND_SUPPLIER = () -> isDark() ? LOGO_BACKGROUND_COLOR_DARK : LOGO_BACKGROUND_COLOR;

    public static final Map<float[], float[]> LIGHT_TO_DARK = ImmutableMap.of(
            new float[]{1.0F, 1.0F, 1.0F}, new float[]{1.0F, 1.0F, 1.0F}, // MC loader
            new float[]{0.0F, 0.0F, 0.5F}, new float[]{0.0F, 0.5F, 1.0F}, // Mod Loader
            new float[]{0.0F, 0.5F, 0.0F}, new float[]{0.0F, 0.5F, 0.0F}, // LOCator
            new float[]{0.5F, 0.0F, 0.0F}, new float[]{1.0F, 0.0F, 0.0F} // MOD - Forge's version color for some reason?
    );

    private static final Pattern DARK_PATTERN = Pattern.compile("darkMojangStudiosBackground:(true|false)");
    private static boolean initialized = false;
    private static boolean isDark;

    private static final Logger LOGGER = LogManager.getLogger("mojangdark");
    static final Map<String, NodeConsumer> CLASSES = ImmutableMap.<String, NodeConsumer>builder()
            .put("net/minecraftforge/fml/loading/progress/ClientVisualization.class", classNode -> {
                MethodNode convertColor = ASMAPI.getMethodNode();
                convertColor.name = "convertColor";
                convertColor.access = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC;
                convertColor.desc = "([F)[F";
                InsnList instructions = convertColor.instructions;
                instructions.add();
                MethodNode methodNode = findMethodNode(classNode, "renderMessage", "(Ljava/lang/String;[FIF)V");
                instructions = new InsnList();
                instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
                instructions.add(getInvokeStaticInsn("convertColor", ""));
                instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));
                methodNode.instructions.insert(instructions);

                MethodNode renderNode = findMethodNode(classNode, "renderBackground", "()V");
                for (AbstractInsnNode insn : renderNode.instructions.toArray()) {
                    if (insn.getOpcode() == Opcodes.INVOKESTATIC) {
                        MethodInsnNode min = (MethodInsnNode) insn;
                        if (Objects.equals(min.owner, "org/lwjgl/opengl/GL11") && Objects.equals(min.name, "glColor4f") && Objects.equals(min.desc, "(FFFF)V")) {
                            renderNode.instructions.insertBefore(insn, getInvokeStaticInsn("drawBackgroundColor", "(FFFF)V"));
                            renderNode.instructions.remove(insn);
                            return true;
                        }
                    }
                }
                return true;
            })
            .build();

    static byte[] transform(String name, byte[] input) {
        ClassNode classNode = new ClassNode();
        new ClassReader(input).accept(classNode, ClassReader.EXPAND_FRAMES);

        boolean modified;
        try {
            modified = CLASSES.get(name).accept(classNode);
        } catch (Exception e) {
            LOGGER.error("Error when transforming", e);
            modified = true;
        }

        if (modified) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(cw);
            return cw.toByteArray();
        }
        return input;
    }

    private static MethodInsnNode getInvokeStaticInsn(String methodName, String descriptor) {
        return new MethodInsnNode(Opcodes.INVOKESTATIC, "me/sizableshrimp/mojangdark/pureshit/DarkScreenLaunchPlugin", methodName, descriptor);
    }

    private static MethodNode findMethodNode(ClassNode classNode, String methodName, String descriptor) throws Exception {
        for (MethodNode methodNode : classNode.methods) {
            if (Objects.equals(methodNode.name, methodName) && Objects.equals(methodNode.desc, descriptor)) {
                return methodNode;
            }
        }
        throw new Exception(String.format("No method node found with name %s and descriptor %s", methodName, descriptor));
    }

    // This is only here to refer to bytecode
    public static float[] convertColor(float[] color) {
        if (isDark()) {
            for (Map.Entry<float[], float[]> entry : LIGHT_TO_DARK.entrySet()) {
                if (Arrays.equals(entry.getKey(), color)) {
                    return entry.getValue();
                }
            }
        }
        return color;
    }

    // Have to accept these floats so that the FLOADs don't need to be removed
    // This is only here to refer to bytecode
    public static void drawBackgroundColor(float red, float green, float blue, float alpha) {
        int packedColor = BRAND_BACKGROUND_SUPPLIER.getAsInt();
        red = red(packedColor) / 255.0F;
        green = green(packedColor) / 255.0F;
        blue = blue(packedColor) / 255.0F;
        alpha = alpha(packedColor) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }

    // This is only here to refer to bytecode
    private static boolean isDark() {
        if (initialized)
            return isDark;
        initialized = true;
        try {
            Path path = Paths.get(".");
            Path optsPath = path.resolve("options.txt");
            if (!Files.exists(path) || !Files.exists(optsPath)) {
                isDark = true;
                return true;
            }
            String joined = String.join("\n", Files.readAllLines(optsPath));
            Matcher matcher = DARK_PATTERN.matcher(joined);
            if (!matcher.find()) {
                isDark = true;
                return true;
            }
            isDark = matcher.group(1).equals("true");
        } catch (IOException e) {
            LOGGER.error("Error when loading options", e);
            isDark = true;
        }
        return isDark;
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

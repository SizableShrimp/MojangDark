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

package me.sizableshrimp.mojangdark.mixins;

import me.sizableshrimp.mojangdark.GameSettingsAccessor;
import me.sizableshrimp.mojangdark.MojangDark;
import net.minecraft.client.GameSettings;
import net.minecraft.nbt.CompoundNBT;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.PrintWriter;
import java.util.Iterator;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings implements GameSettingsAccessor {
    public boolean darkMojangStudiosBackground = true; // Default to true since that's what this entire mod does.

    @Inject(method = "load", locals = LocalCapture.CAPTURE_FAILHARD, allow = 1,
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=toggleSprint")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/GameSettings;toggleSprint:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.BY, by = 2, ordinal = 0))
    private void injectDarkOptionLoad(CallbackInfo ci, CompoundNBT compoundnbt, CompoundNBT compoundnbt1, Iterator var3, String s, String s1) {
        if ("darkMojangStudiosBackground".equals(s)) {
            MojangDark.DARK_MOJANG_STUDIOS_BACKGROUND_COLOR.set((GameSettings) (Object) this, s1);
        }
    }

    @Inject(method = "save", locals = LocalCapture.CAPTURE_FAILHARD, allow = 1,
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=toggleSprint:")),
            at = @At(value = "INVOKE", target = "Ljava/io/PrintWriter;println(Ljava/lang/String;)V", shift = At.Shift.AFTER, ordinal = 0))
    private void injectDarkOptionSave(CallbackInfo ci, PrintWriter printwriter) {
        printwriter.println("darkMojangStudiosBackground:" + MojangDark.DARK_MOJANG_STUDIOS_BACKGROUND_COLOR.get((GameSettings) (Object) this));
    }

    @Override
    public boolean isDarkMojangStudiosBackground() {
        return darkMojangStudiosBackground;
    }

    @Override
    public void setDarkMojangStudiosBackground(boolean darkMojangStudiosBackground) {
        this.darkMojangStudiosBackground = darkMojangStudiosBackground;
    }
}

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

import com.mojang.blaze3d.matrix.MatrixStack;
import me.sizableshrimp.mojangdark.transform.MojangDarkLaunchPlugin;
import net.minecraft.client.gui.LoadingGui;
import net.minecraft.client.gui.ResourceLoadProgressGui;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ResourceLoadProgressGui.class)
public abstract class MixinResourceLoadProgressGui extends LoadingGui {
    @Shadow
    @Final
    private boolean fadeIn;
    @Shadow
    private long fadeOutStart;
    @Shadow
    private long fadeInStart;

    @Unique
    private long k;

    @Inject(method = "render", locals = LocalCapture.CAPTURE_FAILEXCEPTION, allow = 1,
            at = @At(value = "CONSTANT", args = "floatValue=1.0F", ordinal = 0))
    private void injectRender(MatrixStack matrixStack, int arg1, int arg2, float partialTicks, CallbackInfo ci, int i, int j, long k) {
        this.k = k;
    }

    @Redirect(method = "render", allow = 3,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/ResourceLoadProgressGui;fill(Lcom/mojang/blaze3d/matrix/MatrixStack;IIIII)V"))
    private void redirectRender(MatrixStack matrixStack, int minX, int minY, int maxX, int maxY, int color) {
        float f = this.fadeOutStart > -1L ? (k - this.fadeOutStart) / 1000.0F : -1.0F;
        int newColor = MojangDarkLaunchPlugin.BRAND_BACKGROUND_SUPPLIER.getAsInt();
        if (f >= 1.0F) {
            newColor = replaceAlpha(newColor, MathHelper.ceil((1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F)) * 255.0F));
        } else if (this.fadeIn) {
            float f1 = this.fadeInStart > -1L ? (k - this.fadeInStart) / 500.0F : -1.0F;
            newColor = replaceAlpha(newColor, MathHelper.ceil(MathHelper.clamp(f1, 0.15D, 1.0D) * 255.0D));
        }

        fill(matrixStack, minX, minY, maxX, maxY, newColor);
    }

    @Unique
    private static int replaceAlpha(int backgroundColor, int bitShifter) {
        return backgroundColor & 16777215 | bitShifter << 24;
    }
}

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

import me.sizableshrimp.mojangdark.transform.MojangDarkLaunchPlugin;
import net.minecraft.util.ColorHelper;
import net.minecraftforge.fml.client.EarlyLoaderGUI;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EarlyLoaderGUI.class)
public abstract class MixinEarlyLoaderGUI {
    @Redirect(method = "renderBackground", allow = 1, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V", ordinal = 0, remap = false), remap = false)
    private void redirectRenderBackgroundColor(float red, float green, float blue, float alpha) {
        int packedColor = MojangDarkLaunchPlugin.BRAND_BACKGROUND_SUPPLIER.getAsInt();
        red = ColorHelper.PackedColor.red(packedColor) / 255.0F;
        green = ColorHelper.PackedColor.green(packedColor) / 255.0F;
        blue = ColorHelper.PackedColor.blue(packedColor) / 255.0F;
        alpha = ColorHelper.PackedColor.alpha(packedColor) / 255.0F;
        GL11.glColor4f(red, green, blue, alpha);
    }
}

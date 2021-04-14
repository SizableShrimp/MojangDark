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

package me.sizableshrimp.mojangdark;

import me.sizableshrimp.mojangdark.transform.MojangDarkLaunchPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.IntSupplier;

@Mod(MojangDark.MODID)
@Mod.EventBusSubscriber
public class MojangDark {
    public static final String MODID = "mojangdark";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final IntSupplier BRAND_BACKGROUND_SUPPLIER = () -> GameSettingsAccessor.get(Minecraft.getInstance().options).isDarkMojangStudiosBackground()
            ? MojangDarkLaunchPlugin.LOGO_BACKGROUND_COLOR_DARK
            : MojangDarkLaunchPlugin.LOGO_BACKGROUND_COLOR;

    public static final ITextComponent ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND =
            new TranslationTextComponent("options.darkMojangStudiosBackgroundColor.tooltip");
    public static final BooleanOption DARK_MOJANG_STUDIOS_BACKGROUND_COLOR =
            new BooleanOption("options.darkMojangStudiosBackgroundColor", ACCESSIBILITY_TOOLTIP_DARK_MOJANG_BACKGROUND,
                    settings -> GameSettingsAccessor.get(settings).isDarkMojangStudiosBackground(),
                    (settings, optionValues) -> GameSettingsAccessor.get(settings).setDarkMojangStudiosBackground(optionValues));
}

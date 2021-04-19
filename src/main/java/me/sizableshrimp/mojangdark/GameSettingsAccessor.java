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

import me.sizableshrimp.mojangdark.transform.MojangDarkBootstrap;
import net.minecraft.client.GameSettings;
import net.minecraft.client.settings.BooleanOption;
import net.minecraft.util.text.TranslationTextComponent;

public interface GameSettingsAccessor {
    BooleanOption DARK_MOJANG_STUDIOS_BACKGROUND_COLOR = new BooleanOption("options.darkMojangStudiosBackgroundColor",
            new TranslationTextComponent("options.darkMojangStudiosBackgroundColor.tooltip"),
            settings -> get(settings).isDarkMojangStudiosBackground(),
            (settings, optionValues) -> {
                MojangDarkBootstrap.setDark(optionValues);
                get(settings).setDarkMojangStudiosBackground(optionValues);
            });

    boolean isDarkMojangStudiosBackground();

    void setDarkMojangStudiosBackground(boolean darkMojangStudiosBackground);

    @SuppressWarnings("CastToIncompatibleInterface")
    static GameSettingsAccessor get(GameSettings settings) {
        return (GameSettingsAccessor) settings;
    }
}

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MojangDarkBootstrap {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern DARK_PATTERN = Pattern.compile("darkMojangStudiosBackground:(true|false)");
    private static Path gameDir;
    private static boolean initialized = false;
    private static boolean isDark;

    public static Path getGameDir() {
        if (gameDir == null) {
            try {
                Class<?> self = Class.forName("me.sizableshrimp.mojangdark.transform.MojangDarkBootstrap", true, ClassLoader.getSystemClassLoader());
                gameDir = (Path) self.getMethod("getGameDir").invoke(null);
            } catch (Exception e) {
                LOGGER.error("Error loading self!", e);
                gameDir = null;
            }
        }

        return gameDir;
    }

    public static void setGameDir(Path gameDir) {
        MojangDarkBootstrap.gameDir = gameDir;
    }

    public static boolean isDark() {
        if (initialized)
            return isDark;
        // Our mixin to load darkMojangStudiosBackground field in GameSettings doesn't yet exist; so we have to parse options.txt manually.
        // If we can't find the option, default to true since that's what this entire mod does.
        initialized = true;
        try {
            if (getGameDir() == null) {
                LOGGER.warn("No game directory was set. This should not be possible!");
                isDark = true;
                return true;
            }
            Path optsPath = getGameDir().resolve("options.txt");
            if (!Files.exists(getGameDir()) || !Files.exists(optsPath)) {
                isDark = true;
                return true;
            }
            String joined = String.join("\n", Files.readAllLines(optsPath));
            Matcher matcher = DARK_PATTERN.matcher(joined);
            if (!matcher.find()) {
                isDark = true;
                return true;
            }
            isDark = "true".equals(matcher.group(1));
        } catch (IOException e) {
            LOGGER.error("Error when loading options", e);
            isDark = true;
        }
        return isDark;
    }

    public static void setDark(boolean isDark) {
        MojangDarkBootstrap.isDark = isDark;
    }
}

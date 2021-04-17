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

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.loading.LogMarkers;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.List;
import java.util.Map;

public class MojangDarkLocator extends AbstractJarFileLocator {
    private static final Logger LOGGER = LogManager.getLogger();

    public List<IModFile> scanMods() {
        CodeSource codeSource = MojangDarkLocator.class.getProtectionDomain().getCodeSource();
        if (codeSource == null)
            return ImmutableList.of();
        try {
            Path path = Paths.get(codeSource.getLocation().toURI());
            // Only do this if our code source isn't a directory and ends in .jar, which indicates a compiled JAR
            if (!Files.isDirectory(path) && StringUtils.toLowerCase(path.getFileName().toString()).endsWith(".jar")) {
                LOGGER.debug(LogMarkers.SCAN, "Found MojangDark mod jar");
                ModFile modFile = ModFile.newFMLInstance(path, this);
                this.modJars.put(modFile, this.createFileSystem(modFile));
                return ImmutableList.of(modFile);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Error when scanning for MojangDark mod jar", e);
        }
        return ImmutableList.of();
    }

    public String name() {
        return "mojangdark mod jar";
    }

    public void initArguments(Map<String, ?> arguments) {}
}


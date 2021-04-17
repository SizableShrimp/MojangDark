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
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import me.sizableshrimp.mojangdark.MojangDark;
import net.minecraftforge.fml.loading.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

public class MojangDarkTransformationService implements ITransformationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static URL zipUrl;
    private static ZipFile zipFile;

    @Nonnull
    @Override
    public String name() {
        return MojangDark.MODID;
    }

    @Override
    public void initialize(IEnvironment environment) {}

    @Override
    public void beginScanning(IEnvironment environment) {}

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        LOGGER.info("Loading MojangDark transformation service");
        try {
            FakeJarFile.replaceLoaders();
            CodeSource codeSource = MojangDarkTransformationService.class.getProtectionDomain().getCodeSource();
            if (codeSource == null)
                return;
            zipUrl = codeSource.getLocation();
            File file = new File(zipUrl.toURI());
            // Only do this if our code source isn't a directory and ends in .jar, which indicates a compiled JAR
            if (!file.isDirectory() && StringUtils.toLowerCase(file.getName()).endsWith(".jar")) {
                zipFile = new ZipFile(file);
                // Add ourselves to the classpath so that we can reference the launch plugin class in non-dev environments
                ClassLoaderUtils.appendToClassPath(ClassLoader.getSystemClassLoader(), zipUrl);
            }
            // We can only set the gameDir in our bootstrap class AFTER adding ourselves to the classpath in non-dev environments. The order doesn't matter in dev.
            setGameDir(env.getProperty(IEnvironment.Keys.GAMEDIR.get()).orElseGet(() -> Paths.get(".")));
        } catch (Throwable e) {
            LOGGER.error("Error loading transformation service", e);
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    @SuppressWarnings("rawtypes")
    public List<ITransformer> transformers() {
        return ImmutableList.of();
    }

    private static void setGameDir(Path gameDir) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?> bootstrap = Class.forName("me.sizableshrimp.mojangdark.transform.MojangDarkBootstrap", true, ClassLoader.getSystemClassLoader());
        bootstrap.getDeclaredMethod("setGameDir", Path.class).invoke(null, gameDir);
    }
}

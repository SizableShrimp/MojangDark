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

import net.minecraftforge.fml.loading.FMLServiceProvider;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class FakeJarFile extends JarFile {
    public static void replaceLoaders() throws Throwable {
        ArrayList<?> loaders = (ArrayList<?>) Constants.IMPL_LOOKUP.findGetter(Constants.URL_CLASS_PATH_CLASS, "loaders", ArrayList.class).invokeWithArguments(Constants.UCP);

        Class<?> jarLoaderClass = Class.forName(Constants.PACKAGE_NAME + "URLClassPath$JarLoader");
        for (Object loader : loaders) {
            if (jarLoaderClass.isInstance(loader)) {
                JarFile jar = (JarFile) Constants.IMPL_LOOKUP.findGetter(jarLoaderClass, "jar", JarFile.class).invokeWithArguments(loader);
                if (jar != null && Files.isSameFile(Paths.get(jar.getName()), Paths.get(FMLServiceProvider.class.getProtectionDomain().getCodeSource().getLocation().toURI()))) {
                    FakeJarFile fakeJarFile = (FakeJarFile) Constants.UNSAFE.allocateInstance(FakeJarFile.class);
                    fakeJarFile.clone(jar, JarFile.class);
                    Constants.IMPL_LOOKUP.findSetter(jarLoaderClass, "jar", JarFile.class).invokeWithArguments(loader, fakeJarFile);
                }
            }
        }
    }

    // This constructor will be skipped.
    public FakeJarFile(String name) throws IOException {
        super(name);
    }

    public void clone(JarFile jarFile, Class<?> clazz) throws Throwable {
        if (clazz == null || !clazz.isInstance(jarFile)) {
            return;
        }
        Field[] fields = (Field[]) Constants.IMPL_LOOKUP.findVirtual(Class.class, "getDeclaredFields0", MethodType.methodType(Field[].class, boolean.class)).invokeWithArguments(clazz, false);
        for (Field field : fields) {
            if ((field.getModifiers() | Modifier.STATIC) != field.getModifiers()) {
                Constants.IMPL_LOOKUP.findSetter(field.getDeclaringClass(), field.getName(), field.getType())
                        .invokeWithArguments(this, Constants.IMPL_LOOKUP.findGetter(field.getDeclaringClass(), field.getName(), field.getType()).invokeWithArguments(jarFile));
            }
        }
        this.clone(jarFile, clazz.getSuperclass());
    }

    @Override
    public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
        InputStream is = super.getInputStream(ze);
        if (ze == null || !MojangDarkLaunchPlugin.CLASSES.containsKey(ze.getName()))
            return is;

        byte[] classBytes = MojangDarkLaunchPlugin.transform(ze.getName(), IOUtils.toByteArray(is));
        ze.setSize(classBytes.length);
        return new ByteArrayInputStream(classBytes);
    }
}

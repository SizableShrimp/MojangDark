/*
 * Copyright 2020 Alex Thomson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.sizableshrimp.mojangdark.transform;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class ClassLoaderUtils {

    // Java 8
    public static final String URL_CLASS_LOADER_CLASS = "java.net.URLClassLoader";

    // Java 9+
    public static final String BUILTIN_CLASS_LOADER_CLASS = "jdk.internal.loader.BuiltinClassLoader";
    public static final String URL_CLASS_PATH_CLASS = "jdk.internal.loader.URLClassPath";

    public static void appendToClassPath(URL url) throws Throwable {
        appendToClassPath(ClassLoaderUtils.class.getClassLoader(), url);
    }

    @SuppressWarnings("unchecked")
    public static void appendToClassPath(ClassLoader classLoader, URL url) throws Throwable {
        try {
            // Java 8
            Class<?> classLoaderClass = Class.forName(URL_CLASS_LOADER_CLASS);
            if (classLoaderClass.isInstance(classLoader)) {
                // java.net.URLClassLoader.addURL
                Method addURLMethod = classLoaderClass.getDeclaredMethod("addURL", URL.class);
                addURLMethod.setAccessible(true);
                addURLMethod.invoke(classLoader, url);
                return;
            }
        } catch (ClassNotFoundException ex) {
            // no-op
        }

        try {
            // Java 9+
            Class<?> classLoaderClass = Class.forName(BUILTIN_CLASS_LOADER_CLASS);
            Class<?> classPathClass = Class.forName(URL_CLASS_PATH_CLASS);
            if (classLoaderClass.isInstance(classLoader)) {
                Unsafe unsafe = Constants.UNSAFE;

                // jdk.internal.loader.BuiltinClassLoader.ucp
                Field ucpField = classLoaderClass.getDeclaredField("ucp");
                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                Field pathField = classPathClass.getDeclaredField("path");
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

                // Java 9 & 10 - jdk.internal.loader.URLClassPath.urls
                // Java 11 - jdk.internal.loader.URLClassPath.unopenedUrls
                Field urlsField = getField(classPathClass.getDeclaredFields(), "urls", "unopenedUrls");
                long urlsFieldOffset = unsafe.objectFieldOffset(urlsField);
                Collection<URL> urls = (Collection<URL>) unsafe.getObject(ucpObject, urlsFieldOffset);

                // noinspection SynchronizationOnLocalVariableOrMethodParameter
                synchronized (urls) {
                    if (!path.contains(url)) {
                        urls.add(url);
                        path.add(url);
                    }
                }

                return;
            }
        } catch (ClassNotFoundException ex) {
            // no-op
        }

        throw new UnsupportedOperationException("Unsupported ClassLoader");
    }

    public static URL[] getSystemClassPathURLs() throws Throwable {
        return getSystemClassPathURLs(ClassLoaderUtils.class.getClassLoader());
    }

    /**
     * https://stackoverflow.com/questions/46519092/how-to-get-all-jars-loaded-by-a-java-application-in-java9
     * https://github.com/cpw/grossjava9classpathhacks/blob/master/src/main/java/cpw/mods/gross/Java9ClassLoaderUtil.java
     */
    @SuppressWarnings("unchecked")
    public static URL[] getSystemClassPathURLs(ClassLoader classLoader) throws Throwable {
        Objects.requireNonNull(classLoader, "ClassLoader cannot be null");
        try {
            // Java 8
            Class<?> classLoaderClass = Class.forName(URL_CLASS_LOADER_CLASS);
            if (classLoaderClass.isInstance(classLoader)) {
                // java.net.URLClassLoader.getURLs
                Method getURLsMethod = classLoaderClass.getMethod("getURLs");
                return (URL[]) getURLsMethod.invoke(classLoader);
            }
        } catch (ClassNotFoundException ex) {
            // no-op
        }

        try {
            // Java 9+
            Class<?> classLoaderClass = Class.forName(BUILTIN_CLASS_LOADER_CLASS);
            if (classLoaderClass.isInstance(classLoader)) {
                Unsafe unsafe = Constants.UNSAFE;

                // jdk.internal.loader.BuiltinClassLoader.ucp
                Field ucpField = classLoaderClass.getDeclaredField("ucp");
                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);
                return path.toArray(new URL[0]);
            }
        } catch (ClassNotFoundException ex) {
            // no-op
        }

        throw new UnsupportedOperationException("Unsupported ClassLoader");
    }

    private static Field getField(Field[] fields, String... names) throws NoSuchFieldException {
        for (Field field : fields) {
            for (String name : names) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
        }

        throw new NoSuchFieldException(String.join(", ", names));
    }
}

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

package me.sizableshrimp.mojangdark.pureshit;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.net.URLClassLoader;

class Constants {
    static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup IMPL_LOOKUP;

    static final ClassLoader APP_CLASS_LOADER = ClassLoader.getSystemClassLoader();
    static final boolean IS_JAVA_8 = APP_CLASS_LOADER instanceof URLClassLoader;
    static final String PACKAGE_NAME = IS_JAVA_8 ? "sun.misc." : "jdk.internal.loader.";
    static final Class<?> URL_CLASS_PATH_CLASS;
    static final Object UCP;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);

            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            IMPL_LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(implLookupField), UNSAFE.staticFieldOffset(implLookupField));

            URL_CLASS_PATH_CLASS = Class.forName(PACKAGE_NAME + "URLClassPath");
            UCP = IMPL_LOOKUP.findGetter(APP_CLASS_LOADER.getClass().getSuperclass(), "ucp", URL_CLASS_PATH_CLASS).invokeWithArguments(APP_CLASS_LOADER);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}

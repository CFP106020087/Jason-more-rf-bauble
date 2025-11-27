package com.moremod.core;

import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassWriter;

/**
 * 安全的 ClassWriter
 * 解决 ASM 在 Minecraft 环境中的类加载问题
 */
public class SafeClassWriter extends ClassWriter {

    private static final LaunchClassLoader LOADER = (LaunchClassLoader)
            SafeClassWriter.class.getClassLoader();

    public SafeClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        Class<?> c, d;
        try {
            c = Class.forName(type1.replace('/', '.'), false, LOADER);
            d = Class.forName(type2.replace('/', '.'), false, LOADER);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (c.isAssignableFrom(d)) {
            return type1;
        }
        if (d.isAssignableFrom(c)) {
            return type2;
        }
        if (c.isInterface() || d.isInterface()) {
            return "java/lang/Object";
        } else {
            do {
                c = c.getSuperclass();
            } while (!c.isAssignableFrom(d));
            return c.getName().replace('.', '/');
        }
    }
}
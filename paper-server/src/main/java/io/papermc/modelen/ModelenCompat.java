package io.papermc.modelen;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public final class ModelenCompat {
    private ModelenCompat() {}

    public static String check(final File jar) {
        try (JarFile jf = new JarFile(jar)) {
            final Enumeration<? extends ZipEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                final String name = entries.nextElement().getName();
                if (name.startsWith("org/bukkit/craftbukkit/v1_")) {
                    return "Eski NMS desenini kullaniyor (legacy CraftBukkit mapping). Yeni Paper/Modelen surumlerinde kirilabilir.";
                }
                if (name.equals("folia-supported.yml") || name.equals("paper-plugin.yml")) {
                    return "";
                }
            }
        } catch (final Exception ignored) {
            return "";
        }
        return "";
    }
}

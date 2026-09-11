package io.papermc.modelen;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.Bukkit;

public final class ModelenBackup {
    private ModelenBackup() {}

    public static void start() {
        final Thread t = new Thread(ModelenBackup::run, "Modelen-Backup");
        t.setDaemon(true);
        t.start();
    }

    private static void run() {
        try {
            final File dir = new File(ModelenBootstrap.config().backupDir);
            Files.createDirectories(dir.toPath());
            final String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            final File out = new File(dir, "modelen-backup-" + stamp + ".zip");
            try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out))) {
                addWorld(zip, new File("world"));
                addWorld(zip, new File("world_nether"));
                addWorld(zip, new File("world_the_end"));
            }
            System.out.println("[Modelen] Yedek tamamlandi: " + out.getAbsolutePath() + " (" + Math.round(out.length() / 1024.0 / 1024.0 * 10) / 10.0 + " MB)");
        } catch (final Exception e) {
            System.out.println("[Modelen] Yedekleme hata verdi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addWorld(final ZipOutputStream zip, final File worldDir) throws Exception {
        if (!worldDir.exists()) {
            return;
        }
        addRecursive(zip, worldDir, worldDir.getName() + "/");
    }

    private static void addRecursive(final ZipOutputStream zip, final File dir, final String prefix) throws Exception {
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File f : files) {
            if (f.isDirectory()) {
                addRecursive(zip, f, prefix + f.getName() + "/");
            } else {
                zip.putNextEntry(new ZipEntry(prefix + f.getName()));
                Files.copy(f.toPath(), zip);
                zip.closeEntry();
            }
        }
    }
}

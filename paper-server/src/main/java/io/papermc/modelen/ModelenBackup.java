package io.papermc.modelen;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ModelenBackup {
    private ModelenBackup() {}

    public static void registerScheduler() {
        for (final String t : ModelenBootstrap.config().backupTimes) {
            schedule(t);
        }
    }

    private static void schedule(final String hhmm) {
        try {
            final String[] parts = hhmm.split(":");
            final int hh = Integer.parseInt(parts[0]);
            final int mm = Integer.parseInt(parts[1]);
            final java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime next = now.toLocalDate().atTime(hh, mm);
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
            final long delayMs = java.time.Duration.between(now, next).toMinutes() * 60 * 1000;
            final java.util.Timer timer = new java.util.Timer("Modelen-Backup", true);
            timer.schedule(new java.util.TimerTask() {
                @Override public void run() {
                    System.out.println("[Modelen] Planli yedekleme basliyor (" + hhmm + ")...");
                    start();
                }
            }, delayMs, 24L * 60 * 60 * 1000);
        } catch (final Exception e) {
            System.out.println("[Modelen] Yedek saati gecersiz: " + hhmm + " (" + e.getMessage() + ")");
        }
    }

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
            prune(dir);
        } catch (final Exception e) {
            System.out.println("[Modelen] Yedekleme hata verdi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void prune(final File dir) {
        final int keep = ModelenBootstrap.config().keepBackups;
        if (keep <= 0) {
            return;
        }
        final File[] backups = dir.listFiles((d, name) -> name.startsWith("modelen-backup-") && name.endsWith(".zip"));
        if (backups == null || backups.length <= keep) {
            return;
        }
        java.util.Arrays.sort(backups, java.util.Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < backups.length - keep; i++) {
            if (backups[i].delete()) {
                System.out.println("[Modelen] Eski yedek silindi: " + backups[i].getName());
            }
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

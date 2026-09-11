package io.papermc.modelen;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class ModelenRestartSchedule {
    private ModelenRestartSchedule() {}

    public static void register() {
        for (final String t : ModelenBootstrap.config().restartTimes) {
            schedule(t);
        }
    }

    private static void schedule(final String hhmm) {
        try {
            final String[] parts = hhmm.split(":");
            final int hh = Integer.parseInt(parts[0]);
            final int mm = Integer.parseInt(parts[1]);
            final long delayMinutes = initialDelayMinutes(hh, mm);
            final long period = 24L * 60 * 60 * 1000;
            final java.util.Timer timer = new java.util.Timer("Modelen-Restart", true);
            timer.schedule(new java.util.TimerTask() {
                @Override public void run() {
                    restartNow();
                }
            }, delayMinutes * 60 * 1000, period);
        } catch (final Exception e) {
            System.out.println("[Modelen] Restart saati gecersiz: " + hhmm + " (" + e.getMessage() + ")");
        }
    }

    private static long initialDelayMinutes(final int hh, final int mm) {
        final java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime next = now.toLocalDate().atTime(hh, mm);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return java.time.Duration.between(now, next).toMinutes();
    }

    public static void restartNow() {
        restartNow(null);
    }

    public static void restartNow(final CommandSender sender) {
        if (sender != null) {
            sender.sendMessage("[Modelen] Kayit yapiliyor, sunucu 10 saniye icinde duracak. (Otomatik yeniden acilis icin restart scripti kurulu olmali.)");
        }
        Bukkit.broadcastMessage("[Modelen] Sunucu kaydediliyor ve 10 saniye icinde kapatiliyor.");
        final Thread t = new Thread(() -> {
            try {
                Thread.sleep(10_000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "save-all");
            try {
                Thread.sleep(3_000);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "stop");
        }, "Modelen-RestartNow");
        t.setDaemon(true);
        t.start();
    }
}

package io.papermc.modelen;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class ModelenStats {
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private record Sample(long time, double tps, int players) {}

    private static final Deque<Sample> HISTORY = new ArrayDeque<>();

    private ModelenStats() {}

    public static void start() {
        final Thread t = new Thread(() -> {
            while (true) {
                try {
                    synchronized (HISTORY) {
                        HISTORY.add(new Sample(System.currentTimeMillis(), ModelenBootstrap.tps5s(), Bukkit.getOnlinePlayers().size()));
                        prune();
                    }
                } catch (final Throwable ignored) {
                }
                try {
                    Thread.sleep(60_000);
                } catch (final InterruptedException e) {
                    return;
                }
            }
        }, "Modelen-Stats");
        t.setDaemon(true);
        t.start();
    }

    private static void prune() {
        final long cutoff = System.currentTimeMillis() - DAY_MS;
        while (!HISTORY.isEmpty() && HISTORY.peekFirst().time() < cutoff) {
            HISTORY.pollFirst();
        }
    }

    public static void report(final CommandSender sender) {
        final List<Sample> hist;
        synchronized (HISTORY) {
            hist = new ArrayList<>(HISTORY);
        }
        sender.sendMessage("[Modelen] Son " + hist.size() + " dakikalik rapor (" + now() + ")");
        if (hist.isEmpty()) {
            sender.sendMessage("  Kayit yok. 1-2 dakika sonra tekrar deneyin.");
            return;
        }
        double min = 20.0;
        double sum = 0.0;
        int peakPlayers = 0;
        int lowMinutes = 0;
        for (final Sample s : hist) {
            min = Math.min(min, s.tps());
            sum += s.tps();
            peakPlayers = Math.max(peakPlayers, s.players());
            if (s.tps() < ModelenBootstrap.config().tpsAlarmThreshold) {
                lowMinutes++;
            }
        }
        sender.sendMessage(String.format(Locale.ROOT, "  Ortalama TPS: %.1f | En dusuk: %.1f | Dusuk-TPS dakika: %d", sum / hist.size(), min, lowMinutes));
        sender.sendMessage("  Yerel tepe oyuncu sayisi: " + peakPlayers);
        sender.sendMessage("  Detayli izleme: http://127.0.0.1:" + ModelenBootstrap.config().panelPort);
    }

    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}

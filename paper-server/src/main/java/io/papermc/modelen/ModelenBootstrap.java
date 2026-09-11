package io.papermc.modelen;

import ca.spottedleaf.common.time.TickData;
import java.io.File;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.CraftServer;

public final class ModelenBootstrap {
    private static boolean started;
    private static ModelenConfig activeConfig;

    private ModelenBootstrap() {}

    public static void init(final CraftServer server) {
        if (started) {
            return;
        }
        started = true;
        System.out.println("[Modelen] Baslatiliyor (Paper tabanli, " + MinecraftVersion() + " uyumlu)...");
        activeConfig = new ModelenConfig(new File("."));
        ModelenLang.init(activeConfig);
        if (!activeConfig.wizardCompleted) {
            new ModelenWizard(activeConfig).runIfNeeded();
        }
        try {
            server.getCommandMap().register("modelen", new ModelenCommand());
            System.out.println("[Modelen] /modelen komutu kayit edildi.");
        } catch (final Throwable t) {
            System.out.println("[Modelen] Komut kaydi basarisiz: " + t.getMessage());
        }
        ModelenPanel.start(activeConfig);
        ModelenStats.start();
        ModelenRestartSchedule.register();
        ModelenBackup.registerScheduler();
        System.out.println("[Modelen] Tum ozellikler aktif: panel=" + activeConfig.panelEnabled + ", restartTimes=" + activeConfig.restartTimes + ", backupTimes=" + activeConfig.backupTimes);
    }

    public static ModelenConfig config() {
        return activeConfig;
    }

    public static double mspt() {
        final TickData.TickReportData report = MinecraftServer.getServer().tickTimes5s.generateTickReport(
            null, System.nanoTime(), MinecraftServer.getServer().tickRateManager().nanosecondsPerTick());
        return report == null ? 0.0 : report.timePerTickData().segmentAll().average() * 1.0E-6;
    }

    public static double tps5s() {
        final double mspt = mspt();
        return Math.min(20.0, 1000.0 / Math.max(mspt, 0.001));
    }

    public static String MinecraftVersion() {
        return net.minecraft.SharedConstants.getCurrentVersion().id();
    }
}

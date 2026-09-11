package io.papermc.modelen;

import java.io.File;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModelenConfig {
    private static final String FILE_NAME = "modelen.yml";

    private final File file;
    private YamlConfiguration config;

    public boolean panelEnabled;
    public int panelPort;
    public String panelPassword;
    public double tpsAlarmThreshold;
    public int tpsAlarmCooldownMin;
    public String locale;
    public String backupDir;
    public List<? extends String> restartTimes;
    public List<? extends String> backupTimes;
    public int keepBackups;
    public boolean wizardCompleted;
    public String preset;
    public int players;

    public ModelenConfig(final File dataFolder) {
        this.file = new File(dataFolder, FILE_NAME);
        this.load();
    }

    public void load() {
        if (!this.file.exists()) {
            this.config = new YamlConfiguration();
            this.config.set("panel.enabled", true);
            this.config.set("panel.port", 8080);
            this.config.set("panel.password", "");
            this.config.set("locale", "tr");
            this.config.set("alarm.tps-threshold", 15.0);
            this.config.set("alarm.cooldown-minutes", 10);
            this.config.set("backup.directory", "backups");
            this.config.set("backup.times", java.util.List.of());
            this.config.set("backup.keep-last", 14);
            this.config.set("restart.times", java.util.List.of());
            this.config.set("wizard.completed", false);
            this.config.set("preset.name", "unset");
            this.config.set("preset.players", 0);
            this.save();
        }
        this.config = YamlConfiguration.loadConfiguration(this.file);
        final ConfigurationSection panel = this.config.getConfigurationSection("panel");
        this.panelEnabled = panel == null || panel.getBoolean("enabled", true);
        this.panelPort = panel != null ? panel.getInt("port", 8080) : 8080;
        this.backupDir = this.config.getString("backup.directory", "backups");
        this.panelPassword = this.config.getString("panel.password", "");
        this.locale = this.config.getString("locale", "tr");
        this.tpsAlarmThreshold = this.config.getDouble("alarm.tps-threshold", 15.0);
        this.tpsAlarmCooldownMin = this.config.getInt("alarm.cooldown-minutes", 10);
        this.backupTimes = this.config.getStringList("backup.times");
        this.keepBackups = this.config.getInt("backup.keep-last", 14);
        this.restartTimes = this.config.getStringList("restart.times");
        this.wizardCompleted = this.config.getBoolean("wizard.completed", false);
        this.preset = this.config.getString("preset.name", "unset");
        this.players = this.config.getInt("preset.players", 0);
    }

    public void setWizardResult(final String preset, final int players) {
        this.config.set("wizard.completed", true);
        this.config.set("preset.name", preset);
        this.config.set("preset.players", players);
        this.save();
        this.load();
    }

    public void setRestartTimes(final java.util.List<String> times) {
        this.config.set("restart.times", times);
        this.save();
        this.load();
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}

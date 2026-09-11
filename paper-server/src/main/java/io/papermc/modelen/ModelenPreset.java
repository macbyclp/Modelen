package io.papermc.modelen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ModelenPreset {
    public static final List<String> VALID = List.of("smp", "minigame", "skyblock", "anarchy");

    private ModelenPreset() {}

    public static List<String> apply(final String presetName, final int players, final ModelenConfig config) {
        final String preset = normalize(presetName);
        final List<String> notes = new ArrayList<>();
        notes.add("Preset: " + preset + " (" + players + " oyuncu)");
        notes.addAll(applySpigot(preset, players));
        notes.addAll(applyServerProperties(preset));
        return notes;
    }

    public static String normalize(final String raw) {
        final String p = raw == null ? "" : raw.trim().toLowerCase().replace(" ", "");
        return VALID.contains(p) ? p : "smp";
    }

    private static List<String> applySpigot(final String preset, final int players) {
        final List<String> notes = new ArrayList<>();
        final Path spigot = Paths.get("spigot.yml");
        try {
            final YamlConfiguration yml = YamlConfiguration.loadConfiguration(spigot.toFile());
            int vd = 10;
            int sim = 8;
            int spawnRange = 6;
            switch (preset) {
                case "minigame" -> { vd = 8; sim = 6; spawnRange = 4; }
                case "skyblock" -> { vd = 11; sim = 8; spawnRange = 7; }
                case "anarchy" -> { vd = 12; sim = 12; spawnRange = 12; }
                default -> {}
            }
            yml.set("world-settings.default.view-distance", vd);
            yml.set("world-settings.default.simulation-distance", sim);
            yml.set("world-settings.default.mob-spawn-range", spawnRange);
            if (players > 50) {
                yml.set("world-settings.default.entity-bucket-tick-per-block", yml.get("world-settings.default.entity-bucket-tick-per-block", 1));
            }
            yml.save(spigot.toFile());
            notes.add("spigot.yml guncellendi: view-distance=" + vd + ", simulation-distance=" + sim + ", mob-spawn-range=" + spawnRange);
        } catch (final Exception e) {
            notes.add("spigot.yml guncellenemedi: " + e.getMessage());
        }
        return notes;
    }

    private static List<String> applyServerProperties(final String preset) {
        final List<String> notes = new ArrayList<>();
        final Path props = Paths.get("server.properties");
        if (Files.notExists(props)) {
            return notes;
        }
        int vd;
        int sim;
        switch (preset) {
            case "minigame" -> { vd = 8; sim = 6; }
            case "skyblock" -> { vd = 11; sim = 8; }
            case "anarchy" -> { vd = 12; sim = 10; }
            default -> { vd = 10; sim = 8; }
        }
        try {
            final String replaced = Files.readString(props, StandardCharsets.UTF_8)
                .replaceAll("(?m)^view-distance=.*$", "view-distance=" + vd)
                .replaceAll("(?m)^simulation-distance=.*$", "simulation-distance=" + sim);
            Files.writeString(props, replaced, StandardCharsets.UTF_8);
            notes.add("server.properties guncellendi: view-distance=" + vd + ", simulation-distance=" + sim);
        } catch (final IOException e) {
            notes.add("server.properties guncellenemedi: " + e.getMessage());
        }
        return notes;
    }
}

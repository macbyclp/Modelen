package io.papermc.modelen;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;

public final class ModelenBundle {
    private ModelenBundle() {}

    public static void install(final CommandSender sender, final String raw) {
        final String preset = ModelenPreset.normalize(raw);
        final List<String> plugins = switch (preset) {
            case "skyblock" -> List.of("LuckPerms", "SuperiorSkyblock2");
            case "minigame" -> List.of("LuckPerms", "TAB");
            case "anarchy" -> List.of("LuckPerms");
            default -> List.of("EssentialsX", "LuckPerms", "TAB");
        };
        sender.sendMessage("[Modelen] '" + preset + "' bundle kuruluyor: " + String.join(", ", plugins));
        for (final String p : plugins) {
            if (Bukkit.getPluginManager().getPlugin(p) != null) {
                sender.sendMessage("[Modelen] Zaten kurulu: " + p);
                continue;
            }
            ModelenCommand.install(sender, p);
        }
        sender.sendMessage("[Modelen] Bundle tamamlandi. Durum kontrolu: /modelen status");
    }
}

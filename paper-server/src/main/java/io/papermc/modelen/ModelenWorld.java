package io.papermc.modelen;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.CommandSender;

public final class ModelenWorld {
    private ModelenWorld() {}

    public static void handle(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[Modelen] Kullanim: /modelen world <list|create|delete|tp> ...");
            sender.sendMessage("[Modelen]  create <ad> <normal|flat> [seed]");
            sender.sendMessage("[Modelen]  tp <oyuncu> <ad>");
            return;
        }
        final String sub = args[1].toLowerCase();
        switch (sub) {
            case "list" -> {
                sender.sendMessage("[Modelen] Yuklu dunyalar:");
                for (final World w : Bukkit.getWorlds()) {
                    sender.sendMessage("  - " + w.getName() + " (" + w.getEnvironment() + ", oyuncu: " + w.getPlayers().size() + ", chunk: " + w.getLoadedChunks().length + ")");
                }
            }
            case "create" -> {
                if (Bukkit.getWorld(args[2]) != null) {
                    sender.sendMessage("[Modelen] '" + args[2] + "' zaten yuklu.");
                    return;
                }
                final WorldCreator wc = WorldCreator.name(args[2]);
                wc.type(args.length >= 4 && args[3].toLowerCase().contains("flat") ? WorldType.FLAT : WorldType.NORMAL);
                if (args.length >= 5) {
                    try {
                        wc.seed(Long.parseLong(args[4]));
                    } catch (final NumberFormatException ignored) {
                    }
                }
                final World w = Bukkit.createWorld(wc);
                if (w != null) {
                    sender.sendMessage("[Modelen] Dunya olusturuldu: " + w.getName());
                } else {
                    sender.sendMessage("[Modelen] Dunya olusturulamadi.");
                }
            }
            case "delete" -> {
                final World w = Bukkit.getWorld(args[2]);
                if (w == null) {
                    sender.sendMessage("[Modelen] Boyle bir dunya yok.");
                    return;
                }
                if (w.getPlayers().size() > 0) {
                    for (final var p : w.getPlayers()) {
                        final World main = Bukkit.getWorlds().get(0);
                        p.teleport(main.getSpawnLocation());
                    }
                }
                Bukkit.unloadWorld(w, true);
                sender.sendMessage("[Modelen] '" + w.getName() + "' unload edildi. Diskten silme icin klasoru elle kaldirabilirsiniz (yedek alma opsiyonu /modelen backup).");
            }
            case "tp" -> {
                if (args.length < 3) {
                    sender.sendMessage("[Modelen] Kullanim: /modelen world tp <oyuncu> <dunya>");
                    return;
                }
                final World target = Bukkit.getWorld(args[3]);
                if (target == null) {
                    sender.sendMessage("[Modelen] Dunya bulunamadi: " + args[3]);
                    return;
                }
                final org.bukkit.entity.Player p = Bukkit.getPlayerExact(args[2]);
                if (p == null) {
                    sender.sendMessage("[Modelen] Oyuncu bulunamadi: " + args[2]);
                    return;
                }
                p.teleport(target.getSpawnLocation());
                sender.sendMessage("[Modelen] " + p.getName() + " -> " + target.getName());
            }
            default -> sender.sendMessage("[Modelen] Bilinmeyen world opsiyonu: " + sub);
        }
    }

    public static void playerInfo(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("[Modelen] Kullanim: /modelen player <oyuncu>");
            return;
        }
        final org.bukkit.entity.Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) {
            sender.sendMessage("[Modelen] Oyuncu bulunamadi: " + args[1]);
            return;
        }
        final File df = new File("world/playerdata", p.getUniqueId() + ".dat");
        sender.sendMessage("[Modelen] " + p.getName()
            + " | skip ping: " + p.getPing() + "ms"
            + " | dunya: " + p.getWorld().getName()
            + " | gamemode: " + p.getGameMode()
            + " | hp: " + Math.round(p.getHealth())
            + " | playerdata: " + (df.exists() ? "var" : "yok"));
    }
}

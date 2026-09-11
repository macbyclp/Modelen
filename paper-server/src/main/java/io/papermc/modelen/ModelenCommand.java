package io.papermc.modelen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class ModelenCommand extends Command {
    public static final String USAGE = "/modelen <status|preset|bundle|world|player|backup|install|restart|help>";

    protected ModelenCommand() {
        super("modelen");
        this.description = "Modelen sunucu yonetim komutlari";
        this.usageMessage = USAGE;
        this.setPermission("modelen.use");
    }

    @Override
    public boolean execute(@NotNull final CommandSender sender, @NotNull final String label, @NotNull final String[] args) {
        if (!sender.hasPermission("modelen.use")) {
            sender.sendMessage("[Modelen] Bu komut icin yetkiniz yok.");
            return true;
        }
        final String sub = args.length == 0 ? "help" : args[0].toLowerCase();
        switch (sub) {
            case "world" -> ModelenWorld.handle(sender, args);
            case "player" -> ModelenWorld.playerInfo(sender, args);
            case "status" -> this.status(sender);
            case "wizard", "preset" -> {
                if (args.length < 2) {
                    sender.sendMessage("[Modelen] Kullanim: /modelen preset <smp|minigame|skyblock|anarchy> [oyuncu]");
                    sender.sendMessage("[Modelen] Hazir tur: smp, minigame, skyblock, anarchy. Ayarlar restart sonrasi tamamen devreye girer.");
                    return true;
                }
                final String preset = ModelenPreset.normalize(args[1]);
                int players = 20;
                if (args.length >= 3) {
                    try {
                        players = Integer.parseInt(args[2]);
                    } catch (final NumberFormatException e) {
                        players = 20;
                    }
                }
                if (!ModelenPreset.VALID.contains(preset)) {
                    sender.sendMessage("[Modelen] Gecersiz preset: " + args[1]);
                    return true;
                }
                for (final String note : ModelenPreset.apply(preset, players, ModelenBootstrap.config())) {
                    sender.sendMessage("[Modelen] " + note);
                }
                sender.sendMessage("[Modelen] modelen.yml isaretlendi. Cesitli world ayarlari yeniden baslatmada tam uygulanir.");
            }
            case "backup" -> {
                sender.sendMessage("[Modelen] Yedekleme baslatiliyor...");
                ModelenBackup.start();
                sender.sendMessage("[Modelen] Yedek arka planda olusturuluyor. Bittiğinde konsolda bilgi gelecek.");
            }
            case "install" -> {
                if (args.length < 2) {
                    sender.sendMessage("[Modelen] Kullanim: /modelen install <plugin-adi>");
                    return true;
                }
                sender.sendMessage("[Modelen] '" + args[1] + "' plugini Hangar'da aranıyor...");
                final String query = args[1];
                Bukkit.getAsyncScheduler().runNow(null, task -> ModelenCommand.install(sender, query));
            }
            case "bundle" -> {
                if (args.length < 2) {
                    sender.sendMessage("[Modelen] Kullanim: /modelen bundle <smp|minigame|skyblock|anarchy>");
                    return true;
                }
                ModelenBundle.install(sender, args[1]);
            }
            case "restart" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("now")) {
                    ModelenRestartSchedule.restartNow(sender);
                } else {
                    sender.sendMessage("[Modelen] Kullanim: /modelen restart now  |  Planli restart icin modelen.yml'de 'restart.times' listesini duzenleyin. Ornek: ['04:00']");
                }
            }
            default -> {
                sender.sendMessage("[Modelen] Komutlar:");
                sender.sendMessage("  /modelen status            - TPS/MSPT/oyuncu/uptime ozeti");
                sender.sendMessage("  /modelen preset <tur>      - SMP/minigame/skyblock/anarchy profili");
                sender.sendMessage("  /modelen world <list|create|delete|tp> - Dunya yonetimi");
                sender.sendMessage("  /modelen player <isim>     - Oyuncu ozet bilgisi");
                sender.sendMessage("  /modelen bundle <smp|skyblock|minigame> - Hazir plugin paketi kurulumu");
                sender.sendMessage("  /modelen backup            - Dunyalari yedekle (arka plan)");
                sender.sendMessage("  /modelen install <plugin>  - Hangar'dan plugin indir");
                sender.sendMessage("  /modelen restart now       - Kaydet ve sunucuyu durdur");
            }
        }
        return true;
    }

    private void status(final CommandSender sender) {
        final double tps = ModelenBootstrap.tps5s();
        sender.sendMessage("[Modelen] TPS(5s): " + String.format("%.1f", tps) + " | MSPT: " + String.format("%.2f", ModelenBootstrap.mspt()) + " | Oyuncu: " + Bukkit.getOnlinePlayers().size() + " | Aktif plugin: " + activePlugins());
        sender.sendMessage("[Modelen] Urun: Modelen (Paper tabanli) | MC " + MinecraftHY.version);
    }

    private static int activePlugins() {
        int c = 0;
        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.isEnabled()) {
                c++;
            }
        }
        return c;
    }

    private static final class MinecraftHY {
        static final String version = net.minecraft.SharedConstants.getCurrentVersion().id();
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    static void install(final CommandSender sender, final String query) {
        try {
            final String searchUrl = "https://hangar.papermc.io/api/v1/projects/search?limit=1&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            final HttpResponse<String> search = request(searchUrl);
            final String body = search.body();
            if (!body.contains("\"result\"") || body.contains("\"result\":[]")) {
                sender.sendMessage("[Modelen] Hangar'da '" + query + "' icin plugin bulunamadi.");
                return;
            }
            final Pattern ns = Pattern.compile("\"namespace\"\\s*:\\s*\\{[^}]*?\"owner\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"slug\"\\s*:\\s*\"([^\"]+)\"|\"namespace\"\\s*:\\s*\\{[^}]*?\"slug\"\\s*:\\s*\"([^\"]+)\"[^}]*?\"owner\"\\s*:\\s*\"([^\"]+)\"");
            final Matcher m = ns.matcher(body);
            if (!m.find()) {
                sender.sendMessage("[Modelen] Plugin bulundu ama bilgileri okunamadi.");
                return;
            }
            String author;
            String slug;
            if (m.group(1) != null) {
                author = m.group(1);
                slug = m.group(2);
            } else {
                author = m.group(4);
                slug = m.group(3);
            }
            final HttpResponse<String> versions = request("https://hangar.papermc.io/api/v1/projects/" + author + "/" + slug + "/versions?limit=1&offset=0");
            final Matcher vm = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"").matcher(versions.body());
            if (!vm.find()) {
                sender.sendMessage("[Modelen] Sürüm listesi çözümlenemedi: " + author + "/" + slug);
                return;
            }
            final String version = vm.group(1);
            final HttpResponse<InputStream> dl = CLIENT.send(HttpRequest.newBuilder(URI.create(
                "https://hangar.papermc.io/api/v1/projects/" + author + "/" + slug + "/versions/" + URLEncoder.encode(version, StandardCharsets.UTF_8) + "/download")).GET().build(),
                HttpResponse.BodyHandlers.ofInputStream());
            if (dl.statusCode() != 200) {
                sender.sendMessage("[Modelen] İndirme başarısız (HTTP " + dl.statusCode() + ")");
                return;
            }
            final File file = new File("plugins", slug + "-" + version + ".jar");
            try (InputStream in = dl.body(); FileOutputStream out = new FileOutputStream(file)) {
                in.transferTo(out);
            }
            sender.sendMessage("[Modelen] İndirildi: " + file.getName() + " (" + Math.round(file.length() / 1024.0) + " KB).");
            final boolean loaded = tryLoadAndEnable(file);
            if (!loaded) {
                sender.sendMessage("[Modelen] Sıcak yükleme başarısız oldu; plugin sunucu yeniden başlatıldığında devreye girecek.");
            }
            final String compat = ModelenCompat.check(file);
            if (!compat.isEmpty()) {
                sender.sendMessage("[Modelen] Uyum uyarısı: " + compat);
            }
        } catch (final Exception e) {
            sender.sendMessage("[Modelen] Plugin indirilirken hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static HttpResponse<String> request(final String url) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private static boolean tryLoadAndEnable(final File jar) {
        try {
            final Plugin plugin = Bukkit.getPluginManager().loadPlugin(jar);
            if (plugin != null) {
                Bukkit.getPluginManager().enablePlugin(plugin);
                return true;
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

package io.papermc.modelen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class ModelenUpdate {
    private static final String REPO = "macbyclp/Modelen";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private ModelenUpdate() {}

    public static void check(final CommandSender sender) {
        final Thread t = new Thread(() -> {
            try {
                final HttpResponse<String> res = CLIENT.send(HttpRequest.newBuilder(
                    URI.create("https://api.github.com/repos/" + REPO + "/releases/latest")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    sender.sendMessage("[Modelen] Guncelleme kontrolu basarisiz (HTTP " + res.statusCode() + ").");
                    return;
                }
                final String tag = extract(res.body(), "\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
                final String assetUrl = extract(res.body(), "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
                sender.sendMessage("[Modelen] En son surum: " + tag + " | calisan surum: " + ModelenBootstrap.MinecraftVersion() + "-DEV");
                if (assetUrl == null) {
                    sender.sendMessage("[Modelen] Yayinlanmis jar bulunamadi. Manuel derleme icin README'ye bakin.");
                    return;
                }
                sender.sendMessage("[Modelen] Indirme linki: " + assetUrl);
                sender.sendMessage("[Modelen] Otomatik indirmek icin: /modelen update download");
            } catch (final Exception e) {
                sender.sendMessage("[Modelen] Guncelleme kontrolu basarisiz: " + e.getMessage());
            }
        }, "Modelen-UpdateCheck");
        t.setDaemon(true);
        t.start();
    }

    public static void download(final CommandSender sender) {
        final Thread t = new Thread(() -> {
            try {
                final HttpResponse<String> res = CLIENT.send(HttpRequest.newBuilder(
                    URI.create("https://api.github.com/repos/" + REPO + "/releases/latest")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    sender.sendMessage("[Modelen] Surum bilgisi alinamadi (HTTP " + res.statusCode() + ").");
                    return;
                }
                final String assetUrl = extract(res.body(), "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");
                if (assetUrl == null) {
                    sender.sendMessage("[Modelen] Yayinlanmis jar bulunamadi.");
                    return;
                }
                final HttpResponse<InputStream> dl = CLIENT.send(HttpRequest.newBuilder(URI.create(assetUrl)).GET().build(),
                    HttpResponse.BodyHandlers.ofInputStream());
                if (dl.statusCode() != 200) {
                    sender.sendMessage("[Modelen] Indirme basarisiz (HTTP " + dl.statusCode() + ").");
                    return;
                }
                final File current = new File("modelen.jar");
                if (!current.exists()) {
                    sender.sendMessage("[Modelen] 'modelen.jar' dosyasi calisma klasorunde bulunamadi; indirme 'modelen-latest.jar' olarak kaydedildi.");
                } else {
                    Files.copy(current.toPath(), new File("modelen.jar.bak").toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
                try (InputStream in = dl.body(); FileOutputStream out = new FileOutputStream(current.exists() ? current : new File("modelen-latest.jar"))) {
                    in.transferTo(out);
                }
                sender.sendMessage("[Modelen] Indirildi. yeniden baslattiktan sonra yeni sürüm calisir (eski jar 'modelen.jar' ise yedegine kopyalandi).");
            } catch (final Exception e) {
                sender.sendMessage("[Modelen] Guncelleme indirilirken hata: " + e.getMessage());
                e.printStackTrace();
            }
        }, "Modelen-UpdateDownload");
        t.setDaemon(true);
        t.start();
    }

    static String extract(final String json, final String regex) {
        final Matcher m = Pattern.compile(regex).matcher(json);
        return m.find() ? m.group(1) : null;
    }
}

package io.papermc.modelen;

import java.util.HashMap;
import java.util.Map;

public final class ModelenLang {
    private static String locale = "tr";
    private static final Map<String, String> TR = new HashMap<>();
    private static final Map<String, String> EN = new HashMap<>();

    static {
        TR.put("cmd.noPerm", "[Modelen] Bu komut icin yetkiniz yok.");
        TR.put("cmd.backup.start", "[Modelen] Yedekleme baslatiliyor...");
        TR.put("cmd.backup.started", "[Modelen] Yedek arka planda olusturuluyor. Bittiginde konsolda bilgi gelecek.");
        TR.put("cmd.install.usage", "[Modelen] Kullanim: /modelen install <plugin-adi>");
        TR.put("cmd.install.searching", "[Modelen] Araniyor: Hangar'da '%s'...");
        TR.put("cmd.install.notfound", "[Modelen] Hangar'da '%s' icin plugin bulunamadi.");
        TR.put("cmd.preset.usage", "[Modelen] Kullanim: /modelen preset <smp|minigame|skyblock|anarchy> [oyuncu]");
        TR.put("cmd.preset.tups", "[Modelen] Hazir turler: smp, minigame, skyblock, anarchy. Ayarlar restart sonrasi tamasan devreye girer.");
        TR.put("cmd.restart.usage", "[Modelen] Kullanim: /modelen restart now | planli restart icin modelen.yml 'restart.times' duzenleyin.");
        TR.put("alias.keepdelivered", "");
        EN.put("cmd.noPerm", "[Modelen] You do not have permission to use this command.");
        for (final var e : TR.entrySet()) {
            EN.putIfAbsent(e.getKey(), "[Modelen] " + e.getKey());
        }
    }

    public static void init(final ModelenConfig config) {
        locale = config.locale != null ? config.locale.toLowerCase() : "tr";
    }

    public static String t(final String key, final Object... args) {
        final Map<String, String> map = locale.startsWith("en") ? EN : TR;
        String s = map.getOrDefault(key, TR.get(key) != null ? TR.get(key) : key);
        for (int i = 0; i < args.length; i++) {
            s = s.replace("%" + (i + 1), String.valueOf(args[i]));
        }
        return s;
    }
}

package io.papermc.modelen;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Scanner;

public final class ModelenWizard {
    private final ModelenConfig config;

    public ModelenWizard(final ModelenConfig config) {
        this.config = config;
    }

    public void runIfNeeded() {
        if (this.config.wizardCompleted) {
            return;
        }
        final java.io.Console console = System.console();
        Scanner scanner = null;
        try {
            scanner = console != null ? new Scanner(console.reader()) : (System.in.available() > 0 ? new Scanner(System.in) : null);
        } catch (final java.io.IOException e) {
            scanner = null;
        }
        if (scanner == null) {
            System.out.println("[Modelen] Etkilesimli terminal algilanmadi, kurulum sihirbazi atlandi.");
            System.out.println("[Modelen] Sihirbazi bir sonraki acilista calistirmak icin modelen.yml dosyasinda 'wizard.completed' degerini false yapin.");
            return;
        }
        try {
            this.run(scanner);
        } catch (final Exception e) {
            System.out.println("[Modelen] Sihirbaz hata verdi, kurulum atlasildi: " + e.getMessage());
        }
    }

    private void run(final Scanner scanner) {
        System.out.println();
        System.out.println("=========================================================");
        System.out.println("  Modelen Kurulum Sihirbazi (ilk acilis)");
        System.out.println("  Sorulari bos birakirsaniz varsayilan kullanilir.");
        System.out.println("=========================================================");
        System.out.println();
        System.out.print("1/4 Sunucu turu [smp / minigame / skyblock / anarchy] (varsayilan: smp): ");
        final String type = readLine(scanner, "smp").trim().toLowerCase().replace(" ", "");
        System.out.print("2/4 Tahmini ayni anda oyuncu sayisi [sayi] (varsayilan: 20): ");
        final int players = parseInt(readLine(scanner, "20"), 20);
        System.out.print("3/4 Web panelini acik mi baslatayim? [evet/hayir] (varsayilan: evet): ");
        final String panel = readLine(scanner, "evet").trim().toLowerCase();
        System.out.print("4/4 EULA'yi kabul ediyor musunuz? [evet/hayir]: ");
        final String eula = readLine(scanner, "hayir").trim().toLowerCase();

        if (eula.startsWith("e")) {
            acceptEula();
        } else {
            System.out.println("[Modelen] EULA kabul edilmedi. Sunucu acilmadan once 'eula.txt' dosyasinda eula=true olmali.");
        }

        this.applyPreset(typedefined(type), players);

        System.out.println();
        System.out.println("[Modelen] Kurulum tamamlandi!");
        System.out.println("[Modelen] - Sunucu turu: " + type + ", oyuncu kapasitesi: " + players);
        System.out.println("[Modelen] Web panel: " + (panel.startsWith("e") ? "acik (http://127.0.0.1:" + this.config.panelPort + ")" : "kapali"));
        System.out.println("[Modelen] Kullanilabilir komutlar: /modelen status, /modelen backup, /modelen install <plugin>, /modelen restart, /modelen help");
        System.out.println();
        this.config.setWizardResult(typedefined(type), players);

        if (!panel.startsWith("e")) {
            this.config.panelEnabled = false;
            this.config.save();
        }
    }

    private String readLine(final Scanner scanner, final String def) {
        final String line = scanner.nextLine();
        return line == null || line.isBlank() ? def : line.trim();
    }

    private static int parseInt(final String s, final int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (final NumberFormatException e) {
            return def;
        }
    }

    private void acceptEula() {
        try {
            final File eula = new File("eula.txt");
            Files.writeString(eula.toPath(), "eula=true", StandardCharsets.UTF_8);
            System.out.println("[Modelen] eula.txt onaylandi.");
        } catch (final java.io.IOException e) {
            System.out.println("[Modelen] eula.txt yazilamadi: " + e.getMessage());
        }
    }

    private void applyPreset(String preset, final int players) {
        preset = typedefined(preset);
        switch (preset) {
            case "skyblock", "anarchy" -> {
                this.config.players = players;
            }
            case "minigame" -> {
                this.config.players = players;
            }
            default -> {
                this.config.players = players;
            }
        }
    }

    private static String typedefined(final String t) {
        return switch (t) {
            case "minigame" -> "minigame";
            case "skyblock" -> "skyblock";
            case "anarchy" -> "anarchy";
            default -> "smp";
        };
    }
}

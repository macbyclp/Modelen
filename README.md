# Modelen

Modelen, PaperMC tabanlı, yeni başlayanlar ve topluluk sunucuları için tasarlanmış
büyük performanslı bir Minecraft sunucu yazılımıdır.

- **%100 Paper uyumlu**: Tüm Bukkit/Spigot/Paper eklentileri değişiklik sorunsuz çalışır
- **Kolay kurulum**: İndir, çalıştır — ilk açılışta size adım adım yol gösterir
- **Modern Minecraft desteği**: Minecraft 26.2 (son sürüm hattı)

Özellikler
------
- 🧙 **Kurulum sihirbazı**: İlk açılışta sunucu türü (SMP / minigame / skyblock / anarchy),
  kapasite, EULA ve panel sorulur; `spigot.yml` ve `server.properties` otomatik ayarlanır
- 📊 **Dahili web paneli** (`http://127.0.0.1:8080`): canlı TPS grafiği, oyuncu/plugin sayısı,
  son 300 satır log; opsiyonel şifre koruması
- 🚨 **TPS alarmı**: TPS eşik altına düşünce konsol + panel uyarısı
- 💾 **Yedekleme**: Manüel `/modelen backup` veya `backup.times` ile günlük otomatik;
  `keep-last` bekletme politikası
- 🧩 **`/modelen install`**: Hangar'dan tek komutla plugin kurulumu + legacy-NMS uyum taraması
- 📦 **`/modelen bundle`**: Hazır plugin paketleri (EssentialsX + LuckPerms + TAB...)
- 🌍 **`/modelen world`**: Dunya yarat/taşı/sil listele
- 📊 **`/modelen report`**: Son 24 saatlik TPS/oyuncu istatistiği
- ⏰ **Planlı restart**: `restart.times: ["04:00"]`
- 🌐 **i18n**: `locale: tr` veya `en`

Hızlı Başlangıç
------
1. `java 25` kurulu olmalı
2. `modelen.jar` dosyanızı bir klasöre koyun ve başlatın: `java -jar modelen.jar`
3. Kurulum sihirbazını yönlendirin (veya eula.txt'yi elle onaylayın)
4. `http://127.0.0.1:8080` panelini kullanın veya oyun içinde `/modelen help`

Tüm `/modelen` alt komutları:
`status | preset | bundle | world | player | report | backup | install | restart | help`

Nasıl Derleyin (Kaynak Koddan)
------
Derlemek için JDK 21+ (Gradle) ve internet bağlantısı gerekir.

Bu depoyu klonlayın, ardından terminalden şunları çalıştırın:

```
./gradlew applyPatches
./gradlew createPaperclipJar
```

Derlenen sunucu dosyası buradadır: `paper-server/build/libs/paper-paperclip-*.jar`

Bu dosyayı `java -jar` ile doğrudan çalıştırabilirsiniz (Java 25 gerekir).
İlk çalıştırmada vanilla sunucu gerekli dosyaları indirir ve bir kez yamalanır —
sonraki tüm çalıştırmalar tamamen yereldir.

Nasıl Çalıştırın (Sunucu Yöneticileri)
------
1. `paper-paperclip-*.jar` dosyasını bir klasöre koyun
2. `java -jar modelen.jar nogui` ile başlatın
3. `eula.txt` dosyasını onaylayın (ilk çalıştırmada otomatik oluşur)
4. Klasör içine `plugins` dizini ekleyerek eklentilerinizi yerleştirin

Kaynak ve Teşekkür
------
Bu proje, topluluğun en güvenilir sunucu yazılımlarından olan
[PaperMC](https://github.com/PaperMC/Paper)'ninGPL-3.0 lisanslı kod tabanı
üzerine inşa edilmiştir. Orijinal projeye ve tüm katkıda bulunanlara teşekkürler.

- PaperMC: <https://github.com/PaperMC/Paper>
- Minecraft EULA: <https://aka.ms/MinecraftEULA>

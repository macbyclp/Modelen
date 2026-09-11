# Modelen

Modelen, PaperMC tabanlı, yeni başlayanlar ve topluluk sunucuları için tasarlanmış
büyük performanslı bir Minecraft sunucu yazılımıdır.

- **%100 Paper uyumlu**: Tüm Bukkit/Spigot/Paper eklentileri değişiklik sorunsuz çalışır
- **Kolay kurulum**: İndir, çalıştır — ilk açılışta size adım adım yol gösterir
- **Modern Minecraft desteği**: Minecraft 26.2 (son sürüm hattı)

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

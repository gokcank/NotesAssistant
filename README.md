# NotesAssistant

Android için akıllı not asistanı. İngilizce (varsayılan) ve Türkçe dil desteği,
açık/koyu tema seçeneği ve AdMob banner reklam entegrasyonu içerir.

## Özellikler

- **Not alma** — başlık + serbest metin notları; Google Keep tarzı çerçevesiz editör
- **Basit biçimlendirme** — kalın / italik / başlık; hafif işaretleme (`**kalın**`, `*italik*`, `# başlık`), kartlarda biçimli önizleme
- **Checklist** — işaretlenebilir maddeler; sürükleyerek sıralama, tamamlananlar altta toplanır, tek dokunuşla temizlenir
- **Etiketler** — nota tek kategori etiketi (İş, Ev, Alışveriş…); ana ekranda çiplerle filtreleme
- **Gizli notlar** — biyometrik kilit (parmak izi/yüz/ekran kilidi); içerik önizlemesi ve araması kapalı
- **Çöp kutusu** — silinen notlar 30 gün bekletilir; geri alma ve kalıcı silme
- **Akıllı tarih algılama** — not metnindeki "yarın 15:00", "pazartesi", "3 gün sonra", "12.08.2026 saat 10" gibi ifadeler otomatik algılanır ve tek dokunuşla takvim etkinliğine dönüştürülür (`smart/DateTimeExtractor.kt`)
- **Takvime ekleme** — notu tek dokunuşla takvim etkinliğine dönüştürür (sistem takvim uygulaması üzerinden)
- **Paylaşma** — not veya liste editörden metin olarak başka uygulamalara gönderilir
- **Belgeden içe aktarma** — metin tabanlı belgeler (txt, md, …) nota dönüştürülür; ayrıca diğer uygulamalardan "Paylaş" ile metin gönderilebilir
- **Simge kısayolları** — uygulama simgesine uzun basınca "Yeni not" / "Yeni liste"
- **Google buluta yedekleme ve eşitleme**
  - *Drive eşitleme:* Google hesabı bağlanınca notlar Drive'ın uygulamaya özel gizli alanında cihazlar arası eşitlenir (not bazında "en son düzenlenen kazanır"; Google Cloud Console'da Drive API + Android OAuth istemcisi gerektirir)
  - *Otomatik:* Android Auto Backup ile veritabanı kullanıcının Google hesabına yedeklenir (uygulama yeniden kurulunca geri gelir)
  - *Manuel:* JSON dışa/içe aktarma (Google Drive dahil herhangi bir depolama hedefine)

## Mimari

- Kotlin + Jetpack Compose (Material 3), tek modül, MVVM
- Room veritabanı (`notes`, `checklist_items` tabloları)
- `kotlinx.serialization` ile JSON yedekleme
- Min SDK 26 (Android 8.0), Target SDK 36

## Derleme

```
gradlew.bat assembleDebug
```

APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

Cihaza kurmak için: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Gizli değerler ve imzalama

Hassas veriler kaynak kodda tutulmaz; `local.properties` (git dışı) üzerinden okunur:

| Anahtar | Açıklama |
|---|---|
| `ADMOB_APP_ID` / `ADMOB_BANNER_ID` | AdMob kimlikleri → manifest placeholder + `BuildConfig.ADMOB_BANNER_ID` |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Upload keystore yolu ve parolaları (`keystore/` klasörü git dışıdır) |

Değerler tanımlı değilse derleme Google test kimlikleriyle ve imzasız release ile devam eder;
yani depoyu klonlayan herkes gizli dosya olmadan derleyebilir.

## Yayın (Google Play)

- [x] Gerçek AdMob kimlikleri `local.properties` üzerinden bağlandı
- [x] Upload keystore oluşturuldu (`keystore/notesassistant.jks`) ve release imzalama kuruldu
- [x] Release'te R8 küçültme + kaynak daraltma açık (`isMinifyEnabled`, `isShrinkResources`)
- [x] AAB üretimi: `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

Play Console'da sizin yapmanız gerekenler:
- [ ] **Keystore dosyasını ve parolasını yedekleyin** (kaybolursa uygulama güncellenemez!)
- [ ] Uygulama oluşturup AAB'yi Internal testing / Production'a yükleyin
- [ ] Veri güvenliği formu: reklam SDK'sı veri toplar (AdMob "cihaz tanımlayıcıları"); gizlilik politikası URL'si zorunlu
- [ ] AdMob panelinde uygulamayı Play'deki paketle eşleştirin (yayın sonrası)
- [ ] Mağaza girişi: simge, ekran görüntüleri, açıklama (EN + TR)

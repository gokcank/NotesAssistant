# NotesAssistant

*[English](#english) | [Türkçe](#türkçe)*

---

## English

Smart notes assistant for Android. English (default) and Turkish language support,
light/dark theme option, and AdMob banner ad integration.

### Features

- **Note taking** — title + free-text notes; borderless Google Keep–style editor
- **Simple formatting** — bold / italic / heading; lightweight markup (`**bold**`, `*italic*`, `# heading`), formatted preview on cards
- **Checklist** — checkable items; drag to reorder, completed items collect at the bottom, clear with one tap
- **Labels** — a single category label per note (Work, Home, Shopping…); filter chips on the home screen
- **Private notes** — biometric lock (fingerprint/face/screen lock); content preview and search are disabled
- **Trash** — deleted notes are kept for 30 days; restore or delete permanently
- **Smart date detection** — phrases in note text like "tomorrow 3pm", "monday", "in 3 days", "08.12.2026 at 10" are detected automatically and turned into a calendar event with one tap (`smart/DateTimeExtractor.kt`)
- **Add to calendar** — turns a note into a calendar event with one tap (via the system calendar app)
- **Share** — send a note or list as text to other apps from the editor
- **Import from document** — text-based documents (txt, md, …) become notes; text can also be sent in via "Share" from other apps
- **App shortcuts** — long-press the app icon for "New note" / "New list"
- **Cloud backup and sync**
  - *Drive sync:* once a Google account is connected, notes sync across devices via a private app area in Drive ("last edit wins" per note; requires Drive API + an Android OAuth client in Google Cloud Console)
  - *Automatic:* the database is backed up to the user's Google account via Android Auto Backup (restored on reinstall)
  - *Manual:* JSON export/import (to any storage destination, including Google Drive)

### Architecture

- Kotlin + Jetpack Compose (Material 3), single module, MVVM
- Room database (`notes`, `checklist_items` tables)
- JSON backup via `kotlinx.serialization`
- Min SDK 26 (Android 8.0), Target SDK 36

### Build

```
gradlew.bat assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

To install on a device: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Secrets and signing

Sensitive values are never kept in source code; they're read from `local.properties` (not in git):

| Key | Description |
|---|---|
| `ADMOB_APP_ID` / `ADMOB_BANNER_ID` | AdMob IDs → manifest placeholder + `BuildConfig.ADMOB_BANNER_ID` |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Upload keystore path and passwords (`keystore/` folder is not in git) |

If values aren't set, the build falls back to Google's test IDs and an unsigned release —
so anyone who clones the repo can build it without the secret files.

### Release (Google Play)

- [x] Real AdMob IDs wired via `local.properties`
- [x] Upload keystore created (`keystore/notesassistant.jks`) and release signing configured
- [x] R8 minification + resource shrinking enabled for release (`isMinifyEnabled`, `isShrinkResources`)
- [x] AAB build: `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`

Things you need to do in Play Console:
- [ ] **Back up the keystore file and its password** (if lost, the app can no longer be updated!)
- [ ] Create the app and upload the AAB to Internal testing / Production
- [ ] Data safety form: the ad SDK collects data (AdMob "device identifiers"); a privacy policy URL is required
- [ ] Match the app with its Play package in the AdMob console (after release)
- [ ] Store listing: icon, screenshots, description (EN + TR)

---

## Türkçe

Android için akıllı not asistanı. İngilizce (varsayılan) ve Türkçe dil desteği,
açık/koyu tema seçeneği ve AdMob banner reklam entegrasyonu içerir.

### Özellikler

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

### Mimari

- Kotlin + Jetpack Compose (Material 3), tek modül, MVVM
- Room veritabanı (`notes`, `checklist_items` tabloları)
- `kotlinx.serialization` ile JSON yedekleme
- Min SDK 26 (Android 8.0), Target SDK 36

### Derleme

```
gradlew.bat assembleDebug
```

APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

Cihaza kurmak için: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Gizli değerler ve imzalama

Hassas veriler kaynak kodda tutulmaz; `local.properties` (git dışı) üzerinden okunur:

| Anahtar | Açıklama |
|---|---|
| `ADMOB_APP_ID` / `ADMOB_BANNER_ID` | AdMob kimlikleri → manifest placeholder + `BuildConfig.ADMOB_BANNER_ID` |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Upload keystore yolu ve parolaları (`keystore/` klasörü git dışıdır) |

Değerler tanımlı değilse derleme Google test kimlikleriyle ve imzasız release ile devam eder;
yani depoyu klonlayan herkes gizli dosya olmadan derleyebilir.

### Yayın (Google Play)

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

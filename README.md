<div align="center">
  <h1>NotesAssistant 📝</h1>
  <p><strong>A Smart Notes & Checklist App for Android</strong><br><em>Android için Akıllı Not & Liste Uygulaması</em></p>
  <br>

  <img src="reddit_showcase.png" alt="NotesAssistant Showcase" width="100%">
  <br><br>

  [![Android](https://img.shields.io/badge/ANDROID-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
  [![Kotlin](https://img.shields.io/badge/KOTLIN-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Jetpack Compose](https://img.shields.io/badge/JETPACK%20COMPOSE-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![Room](https://img.shields.io/badge/ROOM-4285F4?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/training/data-storage/room)
  [![Google Drive API](https://img.shields.io/badge/GOOGLE%20DRIVE%20API-FFCA28?style=for-the-badge&logo=googledrive&logoColor=black)](https://developers.google.com/drive)<br>
  [![License](https://img.shields.io/badge/LICENSE-MIT-4c1?style=for-the-badge)](LICENSE)
  [![Version](https://img.shields.io/badge/VERSION-V1.0.0-007EC6?style=for-the-badge)](https://github.com/gokcank/NotesAssistant)
</div>

---

## 🇬🇧 English

### Overview
**NotesAssistant** is a modern Android notes and checklist app built with Jetpack Compose. It combines a distraction-free, borderless editor with smart date detection, biometric-locked private notes, and full cross-device sync through the user's own Google Drive account.

### Features
* **Note Taking:** Title + free-text notes with a borderless, Google Keep–style editor.
* **Simple Formatting:** Bold, italic, and headings via lightweight markup (`**bold**`, `*italic*`, `# heading`), with a formatted preview on cards.
* **Checklist:** Checkable items with drag-to-reorder, auto-collecting completed items at the bottom, and one-tap clearing.
* **Labels:** One category label per note (Work, Home, Shopping…) with filter chips on the home screen.
* **Private Notes:** Biometric lock (fingerprint/face/screen lock); locked notes hide their content preview and are excluded from search.
* **Trash:** Deleted notes are kept for 30 days before permanent removal, with restore at any time.
* **Smart Date Detection:** Phrases like "tomorrow 3pm", "monday", or "in 3 days" are detected in note text and converted into a calendar event with one tap.
* **Google Drive Sync:** Notes sync across devices via a private app-data folder in Drive, using a last-edit-wins merge strategy.
* **Backup:** Automatic Android Auto Backup plus manual JSON export/import.
* **Sharing & Shortcuts:** Share notes as text to other apps; long-press the app icon for quick "New note" / "New list".
* **Theme Selection:** Light, Dark, or System.

### Tech Stack
* **Language:** Kotlin
* **UI:** Jetpack Compose, Material 3
* **Architecture:** MVVM, single module
* **Local Storage:** Room (`notes`, `checklist_items`), DataStore Preferences
* **Sync:** Google Drive API (app-data scope) via Play Services Identity/Authorization
* **Security:** AndroidX Biometric for private-note authentication
* **Serialization:** kotlinx.serialization
* **Min SDK:** 26 · **Target SDK:** 36

### Privacy & Security
All secrets (AdMob IDs, signing keystore passwords) are kept out of source control via `local.properties` and are never hardcoded. The project builds successfully even without these keys — it falls back to Google's test ad IDs and an unsigned release — so anyone can clone and build the app. Google Drive sync only ever requests the app-private `drive.appdata` scope; it never has access to the rest of the user's Drive.

### Build
```
gradlew.bat assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

To install on a device: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Secrets Table

| Key | Description |
|---|---|
| `ADMOB_APP_ID` / `ADMOB_BANNER_ID` | AdMob IDs → manifest placeholder + `BuildConfig.ADMOB_BANNER_ID` |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Upload keystore path and passwords (`keystore/` folder is not in git) |

### Release Checklist (Google Play)
- [x] Real AdMob IDs wired via `local.properties`
- [x] Upload keystore created (`keystore/notesassistant.jks`) and release signing configured
- [x] R8 minification + resource shrinking enabled for release (`isMinifyEnabled`, `isShrinkResources`)
- [x] AAB build: `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [ ] **Back up the keystore file and its password** (if lost, the app can no longer be updated!)
- [ ] Create the app and upload the AAB to Internal testing / Production
- [ ] Data safety form: the ad SDK collects data (AdMob "device identifiers"); a privacy policy URL is required
- [ ] Match the app with its Play package in the AdMob console (after release)
- [ ] Store listing: icon, screenshots, description (EN + TR)

---

## 🇹🇷 Türkçe

### Genel Bakış
**NotesAssistant**, Jetpack Compose ile geliştirilmiş modern bir Android not ve liste uygulamasıdır. Sade, çerçevesiz bir editörü; akıllı tarih algılamayı, biyometrik kilitli gizli notları ve kullanıcının kendi Google Drive hesabı üzerinden tam cihazlar arası eşitlemeyi bir araya getirir.

### Özellikler
* **Not Alma:** Google Keep tarzı çerçevesiz editörle başlık + serbest metin notları.
* **Basit Biçimlendirme:** Hafif işaretlemeyle kalın, italik ve başlık (`**kalın**`, `*italik*`, `# başlık`); kartlarda biçimli önizleme.
* **Checklist:** Sürükleyerek sıralanabilen maddeler; tamamlananlar kendiliğinden altta toplanır, tek dokunuşla temizlenir.
* **Etiketler:** Nota tek kategori etiketi (İş, Ev, Alışveriş…); ana ekranda çiplerle filtreleme.
* **Gizli Notlar:** Biyometrik kilit (parmak izi/yüz/ekran kilidi); kilitli notların içerik önizlemesi gizlenir ve aramaya dahil edilmez.
* **Çöp Kutusu:** Silinen notlar kalıcı silinmeden önce 30 gün bekletilir, istenildiği an geri alınabilir.
* **Akıllı Tarih Algılama:** Not metnindeki "yarın 15:00", "pazartesi", "3 gün sonra" gibi ifadeler algılanır ve tek dokunuşla takvim etkinliğine dönüştürülür.
* **Google Drive Eşitleme:** Notlar, Drive'ın uygulamaya özel gizli alanı üzerinden "en son düzenlenen kazanır" mantığıyla cihazlar arası eşitlenir.
* **Yedekleme:** Otomatik Android Auto Backup ve manuel JSON dışa/içe aktarma.
* **Paylaşma & Kısayollar:** Notları metin olarak başka uygulamalara paylaşma; simgeye uzun basarak hızlı "Yeni not" / "Yeni liste".
* **Tema Seçimi:** Açık, Koyu veya Sistem.

### Kullanılan Teknolojiler
* **Dil:** Kotlin
* **Arayüz:** Jetpack Compose, Material 3
* **Mimari:** Tek modül, MVVM
* **Yerel Depolama:** Room (`notes`, `checklist_items`), DataStore Preferences
* **Eşitleme:** Play Services Identity/Authorization üzerinden Google Drive API (app-data kapsamı)
* **Güvenlik:** Gizli not doğrulaması için AndroidX Biometric
* **Serileştirme:** kotlinx.serialization
* **Min SDK:** 26 · **Target SDK:** 36

### Gizlilik & Güvenlik
Tüm gizli değerler (AdMob kimlikleri, imza anahtarı parolaları) `local.properties` aracılığıyla kaynak kod dışında tutulur ve asla doğrudan koda yazılmaz. Proje bu anahtarlar olmadan da sorunsuz derlenir — Google'ın test reklam kimlikleriyle ve imzasız bir release'e düşer — böylece depoyu klonlayan herkes uygulamayı derleyebilir. Google Drive eşitlemesi yalnızca uygulamaya özel `drive.appdata` kapsamını ister; kullanıcının Drive'ının geri kalanına asla erişemez.

### Derleme
```
gradlew.bat assembleDebug
```
APK çıktısı: `app/build/outputs/apk/debug/app-debug.apk`

Cihaza kurmak için: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

### Gizli Değerler Tablosu

| Anahtar | Açıklama |
|---|---|
| `ADMOB_APP_ID` / `ADMOB_BANNER_ID` | AdMob kimlikleri → manifest placeholder + `BuildConfig.ADMOB_BANNER_ID` |
| `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | Upload keystore yolu ve parolaları (`keystore/` klasörü git dışıdır) |

### Yayın Kontrol Listesi (Google Play)
- [x] Gerçek AdMob kimlikleri `local.properties` üzerinden bağlandı
- [x] Upload keystore oluşturuldu (`keystore/notesassistant.jks`) ve release imzalama kuruldu
- [x] Release'te R8 küçültme + kaynak daraltma açık (`isMinifyEnabled`, `isShrinkResources`)
- [x] AAB üretimi: `gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`
- [ ] **Keystore dosyasını ve parolasını yedekleyin** (kaybolursa uygulama güncellenemez!)
- [ ] Uygulama oluşturup AAB'yi Internal testing / Production'a yükleyin
- [ ] Veri güvenliği formu: reklam SDK'sı veri toplar (AdMob "cihaz tanımlayıcıları"); gizlilik politikası URL'si zorunlu
- [ ] AdMob panelinde uygulamayı Play'deki paketle eşleştirin (yayın sonrası)
- [ ] Mağaza girişi: simge, ekran görüntüleri, açıklama (EN + TR)

---

<div align="center">
  <img src="logo.png" alt="NotesAssistant Logo" width="120">
</div>

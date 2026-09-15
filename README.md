# Wiantex Native Android

Wiantex'in WebView kullanmayan Android istemcisi.

## Teknoloji

- Kotlin
- Jetpack Compose / Material 3
- PHP session cookie ile mevcut Wiantex hesabı
- JSON native API
- GitHub Actions APK build
- minSdk 24 / targetSdk 37
- AGP 9.4.0 / Gradle 9.6.0 / JDK 17

## Şu an native olan ekranlar

- Giriş / oturum geri yükleme
- Forum konu akışı
- Pulses akışı ve beğeni
- Mesaj konuşması + mesaj gönderme
- Market ürünleri + Woin ile ürün satın alma
- Profil
- Bildirimler

`WebView` bağımlılığı veya WebView ekranı yoktur.

## Backend kurulumu

Android uygulaması şu URL'leri kullanır:

- `/api/native/login`
- `/api/native/me`
- `/api/native/forum`
- `/api/native/messages`
- `/api/native/profile`
- `/api/native/notifications`
- `/api/native/pulses`
- `/api/native/market`

Mevcut Wiantex kaynağında ilk altı native PHP dosyası bulunuyor. Bunların route kayıtlarını ve yeni Pulses/Market endpointlerini eklemek için `backend_patch/README.md` dosyasını uygula.

> Backend patch uygulanmadan uygulama API çağrıları 404 döner.

## GitHub repo

```bash
git init
git add .
git commit -m "Wiantex native Android v1"
git branch -M main
git remote add origin https://github.com/KULLANICI/Wiantex-Android.git
git push -u origin main
```

Push sonrası `.github/workflows/android.yml` GitHub Actions üzerinde debug APK üretir.

APK: **Actions > ilgili build > Artifacts > Wiantex-Native-debug**

## Android Studio

Projeyi Android Studio ile klasör olarak aç. JDK 17 seç ve Gradle Sync çalıştır.

CLI'da sisteminde Gradle 9.6 kuruluysa:

```bash
gradle :app:assembleDebug
```

Çıktı:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## API adresini değiştirmek

`app/build.gradle.kts`:

```kotlin
buildConfigField("String", "API_BASE_URL", "\"https://www.wiantex.com/\"")
```

Test subdomaini kullanacaksan yalnızca bu değeri değiştir.

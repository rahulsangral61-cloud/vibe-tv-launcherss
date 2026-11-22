# Vibe TV Launcher

Apple TV jaisa clean, dynamic home screen for Android TV / Google TV (Vu TV pe test karne ke liye bana), saath me ADB scripts jo bloatware aur ads hatate hain.

## Features

- Full-screen home with **colour wash jo focused app ke icon/banner se change hota hai**
- Bade focus-scale cards, glow aur white border (D-pad friendly)
- Greeting + live clock
- **Favourites**: kisi bhi app pe OK **hold** karo to pin / unpin
- Quick buttons: Settings, Wi-Fi, Bluetooth, Display
- Naye install / uninstall hue apps automatically update hote hain
- Back button launcher se bahar nahi nikalta

Stack: Kotlin + Jetpack Compose for TV (`androidx.tv:tv-material`), minSdk 24.

## Setup

### 1. TV pe

1. Settings > Device Preferences > About > **Build** pe 7 baar OK dabao
2. Settings > Device Preferences > **Developer options** > USB debugging ON
3. Wireless debugging ON karo (Android 11+). Purane TV pe "Network debugging" / "ADB over network" ON karo.
4. TV aur computer **same Wi-Fi** pe hone chahiye

### 2. Computer pe

[Platform-tools](https://developer.android.com/tools/releases/platform-tools) install karo (`adb` PATH me hona chahiye).

```bash
cd scripts
./connect.sh                      # auto-discover, ya IP puchega
./connect.sh 192.168.1.50         # port 5555
./connect.sh pair 192.168.1.50:37001 123456   # Android 11+ pairing (TV pe jo IP:port + code dikhe)
./connect.sh 192.168.1.50:41234   # pairing ke baad TV pe dikhne wala connect port
```

TV pe "Allow USB debugging?" aaye to **Always allow**.

Windows pe bash na ho to seedha commands chalao: `adb pair`, `adb connect`, `adb install -r`, `adb shell ...` (scripts me wahi hain).

### 3. Build

Android Studio me repo open karo (Gradle wrapper wo khud bana lega), phir **Build > Build APK(s)**. Ya terminal se:

```bash
gradle wrapper --gradle-version 8.9   # sirf pehli baar
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### 4. Install aur default launcher banao

```bash
cd scripts
./launcher.sh install
./launcher.sh activate      # stock launcher disable karta hai, Home ab Vibe Launcher kholega
./launcher.sh status
```

Kuch gadbad lage to:

```bash
./launcher.sh restore-stock
```

> Google TV / newer Android TV default launcher change karne nahi dete, isliye `activate` stock launcher ko **disable** karta hai. Pehle `install` zaroor karo, warna Home screen khaali reh sakti hai. Agar phir bhi atak jao: `adb shell pm enable com.google.android.tvlauncher` (Google TV pe `com.google.android.apps.tv.launcherx`).

### 5. Bloatware / ads hatao

```bash
cd scripts
./debloat.sh audit      # saare packages packages-dump.txt me
# packages.txt me unwanted packages likho
./debloat.sh apply
./debloat.sh restore    # undo
```

- `pm uninstall -k --user 0` use hota hai: sirf current user se hatata hai, system partition me app rehti hai. Factory reset se sab wapas.
- Kuch critical packages (systemui, settings, Play Store, GMS, stock launcher...) script khud protect karti hai.
- Vu-specific ad/promo packages ke naam TV model pe depend karte hain, isliye `packages.txt` me default sirf generic Google extras hain. `audit` ka output dekh ke apni list banao.

## Project layout

```
app/src/main/java/com/vibe/tvlauncher/
  MainActivity.kt        # HOME activity, favourites, app launch, package change receiver
  data/AppRepository.kt  # installed apps + banner/icon + accent colour
  ui/HomeScreen.kt       # saara Compose UI
  ui/Theme.kt
scripts/
  connect.sh  launcher.sh  debloat.sh  packages.txt
```

## Roadmap ideas

- Weather / news widget aur "Top Shelf" jaisa hero carousel
- Custom wallpapers, screensaver
- Apps ko reorder karna, hide karna
- Hardware keys / voice button handling

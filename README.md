# Antigravity Android App (Telefoon & Pixel 9 Pro Fold)

Een native Android-app (Kotlin + Jetpack Compose + Material 3 Adaptive) waarmee je **Antigravity** (en andere web-apps) als volwaardige Android-app gebruikt — geoptimaliseerd voor zowel een gewone Android-telefoon als een **Google Pixel 9 Pro Fold**.

## Belangrijkste functies

- **URL invoeren of QR-code scannen:** Koppel direct je Antigravity-omgeving door de URL in te typen, een link vanuit een andere app te delen naar *Antigravity*, of een QR-code te scannen met de ingebouwde offline CameraX + ZXing QR-scanner (inclusief zaklamp en camera-wissel).
- **Hybride Architectuur (Veilige Google-login + Dual-Pane WebView):**
  - **Veilige Modus (Chrome Custom Tabs):** Deelt veilig je Chrome-sessie zodat **Google Sign-In / OAuth**, Passkeys, 2FA en Web Push altijd probleemloos werken (zonder `403 disallowed_useragent`). Op een opengeklapte **Pixel 9 Pro Fold** ondersteunt dit een **Partial Custom Tab Side-Sheet** (Hub links, Antigravity rechts) én **Android Split-Screen** (`FLAG_ACTIVITY_LAUNCH_ADJACENT`) voor twee sessies naast elkaar.
  - **Native WebView Modus:** Volledig scherm zonder adresbalk, **in-app Dual-Pane** met versleepbare & scharnier-uitgelijnde scheidingslijn, pull-to-refresh, aparte zoom per scherm, bestandsupload/download, camera/microfoon-toegang en Android-meldingen. Detecteert Google OAuth automatisch en biedt met één tik de Veilige Modus aan.
- **Pixel 9 Pro Fold & Telefoon optimalisatie:**
  - **App Continuity:** Bij het open- en dichtklappen van je Pixel 9 Pro Fold blijft je actieve sessie en invoer behouden zonder te herladen.
  - **Afgestemde breakpoints (`600dp`):** Herkent automatisch het verschil tussen een gewone telefoon / het 6.3" coverscherm (~412dp) en het bijna vierkante 8.0" binnenscherm (~700–800dp).
  - **Boek- & Tabletop-modus:** Detecteert de scharnierpositie via `Jetpack WindowManager` (`FoldingFeature`).
  - **Aparte weergave-instellingen:** Eigen zoompercentage voor het coverscherm en voor het opengeklapte binnenscherm.
- **Meldingen:** Android 13+ notificatiekanaal, web-notificatie JS-bridge, testmelding-knop en optionele *Achtergrond-bewaking* (Foreground Service).

## APK downloaden & installeren

### Via GitHub Actions (aanbevolen)
1. Open het tabblad **Actions** in deze GitHub-repository en klik op de meest recente succesvolle run van **Android Build & APK**.
2. Download onder **Artifacts** het pakket **`Antigravity-APK`** en pak `Antigravity-debug.apk` uit (of download direct vanaf **Releases** bij een `v*`-tag).
3. Open `Antigravity-debug.apk` op je telefoon of Pixel 9 Pro Fold en sta installeren vanuit onbekende bron toe wanneer Android daarom vraagt.

### Zelf lokaal bouwen
Vereisten: JDK 21 & Android SDK (API 35).

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

De installeerbare APK staat vervolgens op:
`app/build/outputs/apk/debug/app-debug.apk`
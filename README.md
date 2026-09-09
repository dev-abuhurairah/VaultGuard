# 🛡️ VaultGuard — Production-Grade Android Password Manager (Android 13+)

A personal-use, zero-knowledge, production-grade Android Password Manager built natively with **Kotlin**, **Jetpack Compose (Material 3)**, **Android Keystore**, and **Dual-Engine Auto-Detection (Autofill Framework + Accessibility Service + Overlay)** for Android 13, 14, and 15+.

---

## 🌟 Inspirations & Key Benchmarks

VaultGuard brings together the finest security and UX architectural choices from the world's leading password managers:

1. **Bitwarden**: Dual-engine autofill architecture (`AutofillService` + `AccessibilityService` fallback for non-standard webviews/apps), Quick Settings notification tile, built-in TOTP authenticator.
2. **1Password**: Categorized vaults (Logins, Secure Notes, Credit Cards, Identities, Wi-Fi), "Watchtower" security audit (weak/duplicate/compromised passwords), elegant Material 3 card hierarchy.
3. **KeePassDX**: Offline-first zero-knowledge architecture, Argon2id/PBKDF2 key derivation, hardware Keystore wrapping, sensitive clipboard isolation (`ClipDescription.EXTRA_IS_SENSITIVE`), auto-clear timers.
4. **Dashlane & Proton Pass**: Instant floating overlay prompt upon field focus ("*Autofill with VaultGuard?*"), automated prompt upon registration/login ("*Save new credentials?*"), Credential Manager API readiness.

---

## 🚀 Key Features

* **Dual-Engine Auto-Detection & Prompt**:
  * **Native Autofill Service (`VaultAutofillService`)**: Official Android Autofill provider with inline keyboard suggestions.
  * **Real-Time Accessibility Engine (`VaultAccessibilityService`)**: Detects username and password input fields across all apps and browsers (Chrome, Firefox, Edge, banking apps, webviews) where standard autofill is blocked.
  * **Floating Overlay Prompt (`PromptDialogActivity` / `FloatingPromptService`)**: When a field is focused, a sleek card prompts: *"Confirm Autofill for [App]?"*. A single biometric tap fills username and password automatically!
  * **Auto-Detect & Prompt-to-Save**: When you enter a new password and tap "Sign In" or "Submit", VaultGuard detects the submission and prompts: *"Save login for [App] to Vault?"*.
* **Zero-Knowledge Hardware-Backed Cryptography**:
  * Derived Master Key: **PBKDF2 with HMAC-SHA256 (600,000 rounds)** and random 32-byte salt.
  * Vault Database: Encrypted using **AES-256 in Galois/Counter Mode (GCM)** with unique 12-byte initialization vectors (IV) and 128-bit authentication tags.
  * Hardware Keystore: Master key wrapped with **Android Keystore** (`StrongBox` / `TEE`).
  * **Anti-Screenshot & Anti-Recording**: `FLAG_SECURE` window flag prevents screenshots and blanks recent apps switcher previews.
  * **Secure Clipboard**: Sets `ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+ (stops system clipboard preview leaks) and auto-clears copied passwords after 30s/60s.
* **Integrated 2FA Authenticator (TOTP)**:
  * Full RFC 6238 compliant Time-Based One-Time Password generator.
  * Live animated circular progress ring with 30-second countdown.
  * 1-tap copy to clipboard.
* **Advanced Password & Passphrase Generator**:
  * Configurable length (8 to 64 chars) with toggles for Uppercase, Lowercase, Digits, Symbols, and Exclude Ambiguous (`0/O, 1/l`).
  * Passphrase mode (Diceware style) for human-memorable multi-word phrases.
  * Shannon Entropy meter with strength rating (Weak, Fair, Good, Military-grade).
* **Watchtower Security Audit**:
  * Scans vault for weak passwords, reused credentials across different services, and missing 2FA.
* **Encrypted Backup & Restore**:
  * Export password-encrypted JSON backup files and restore anytime.

---

## 🔐 Permissions Required & Why

| Permission | Android API | Purpose |
| :--- | :--- | :--- |
| `android.permission.BIND_AUTOFILL_SERVICE` | Android 8.0+ | Official system autofill service provider. |
| `android.permission.BIND_ACCESSIBILITY_SERVICE` | All APIs | Deep detection of password inputs and form submits in browsers/apps. |
| `android.permission.SYSTEM_ALERT_WINDOW` | All APIs | Displays floating confirmation prompts over other apps. |
| `android.permission.USE_BIOMETRIC` | All APIs | Unlocks vault credentials via fingerprint/face recognition. |
| `android.permission.POST_NOTIFICATIONS` | Android 13+ (API 33+) | Instant heads-up alerts for auto-save and clipboard clear timers. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | All APIs | Prevents aggressive OEM battery killers from killing autofill. |
| `android.permission.QUERY_ALL_PACKAGES` | Android 11+ | Identifies the target app's name and icon for the prompt UI. |

---

## 🛠️ How to Open & Build in Android Studio

1. Open **Android Studio** (Hedgehog, Iguana, Jellyfish, Koala or newer).
2. Click **File -> Open...** and select:
   ```
   C:\Users\abuhu\.gemini\antigravity\scratch\vaultguard-android
   ```
3. Allow Gradle to sync dependencies.
4. Connect your Android 13+ device (or launch an Android 13/14 emulator).
5. Click **Run (`Shift + F10`)**.

---

## 📱 First-Time Setup Flow

1. **Permissions Wizard**: Upon first launch, the app guides you through granting **Autofill Service**, **Accessibility Service**, **Appear on Top**, and **Notifications**. Each item has a direct 1-tap shortcut to system settings.
2. **Master Password**: Set a strong master password (or authenticate with your device fingerprint).
3. **Enjoy seamless autofill & auto-save**: Open any app or browser (e.g. Instagram, Netflix, or Chrome), focus a password field, and watch VaultGuard prompt you!

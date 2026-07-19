# LT_9904 Build Guide (Android 15)

This guide documents the configuration required to build, sign, and install the native Android application on the target **LT_9904** hardware device running **Android 15 (API Level 35)**.

## Build Requirements
- **JDK:** Version 17
- **Gradle:** Version 8.7+
- **Android Gradle Plugin (AGP):** Version 8.4.0 (optimized for Gradle 9+ environment compatibility)

## Signing Instructions for LT_9904
To generate a valid release APK installable on the LT_9904, provide the following GitHub Secrets in your repository:
- `CM_KEYSTORE_BASE64`: Base64 encoded `.keystore` or `.jks` file.
- `CM_KEYSTORE_PASSWORD`: Keystore decryption password.
- `CM_KEY_ALIAS`: Alias of the signing key.

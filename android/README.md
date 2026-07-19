# Native Android Application Source Module

The core of the native Android application is situated in the `/app` directory, following standard Android project architecture.

## Package Packages
- `com.mexc.mariabot.ui`: Jetpack Compose views, themes, and screen definitions.
- `com.mexc.mariabot.model`: Domain data models.
- `com.mexc.mariabot.network`: official `MexcApiService` declarations.
- `com.mexc.mariabot.database`: local native SQLite database operations.
- `com.mexc.mariabot.repository`: unified data coordinate logic.
- `com.mexc.mariabot.util`: cryptographic signature helper routines.

package com.mexc.mariabot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mexc.mariabot.database.AppDatabaseHelper
import com.mexc.mariabot.network.MexcApiService
import com.mexc.mariabot.repository.BotRepository
import com.mexc.mariabot.ui.MariaBotViewModel
import com.mexc.mariabot.ui.screens.DashboardScreen
import com.mexc.mariabot.ui.theme.MariaBotTheme
import com.mexc.mariabot.worker.BotStatusWorker
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Local SQLite Database Helper
        val dbHelper = AppDatabaseHelper(applicationContext)

        // 2. Initialize Notification Channels & Request Permission on API 33+
        com.mexc.mariabot.util.NotificationCenter.initChannels(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, permission) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            }
        }

        // 3. Initialize official MEXC Retrofit API Service
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mexc.com/") // Official MEXC Spot/Futures Base Endpoint
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(MexcApiService::class.java)

        // 3. Initialize Repository & ViewModel via standard Constructor Injection
        val repository = BotRepository(dbHelper, apiService)
        val viewModel = MariaBotViewModel(repository)

        // 4. Schedule Background Periodic Telemetry Checks via WorkManager
        try {
            val statusWorkRequest = PeriodicWorkRequestBuilder<BotStatusWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                "MariaBotStatusTelemetry",
                ExistingPeriodicWorkPolicy.KEEP,
                statusWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            MariaBotTheme {
                DashboardScreen(viewModel)
            }
        }
    }
}

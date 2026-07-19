package com.mexc.mariabot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mexc.mariabot.database.AppDatabaseHelper
import com.mexc.mariabot.network.MexcApiService
import com.mexc.mariabot.repository.BotRepository
import com.mexc.mariabot.ui.MariaBotViewModel
import com.mexc.mariabot.ui.screens.DashboardScreen
import com.mexc.mariabot.ui.theme.MariaBotTheme
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Local SQLite Database Helper
        val dbHelper = AppDatabaseHelper(applicationContext)

        // 2. Initialize official MEXC Retrofit API Service
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mexc.com/") // Official MEXC Spot/Futures Base Endpoint
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(MexcApiService::class.java)

        // 3. Initialize Repository & ViewModel via standard Constructor Injection
        val repository = BotRepository(dbHelper, apiService)
        val viewModel = MariaBotViewModel(repository)

        setContent {
            MariaBotTheme {
                DashboardScreen(viewModel)
            }
        }
    }
}

package com.mexc.mariabot.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mexc.mariabot.database.AppDatabaseHelper
import com.mexc.mariabot.model.BotLog
import java.util.UUID

class BotStatusWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dbHelper = AppDatabaseHelper(applicationContext)
        val activeCount = dbHelper.getAllPositions().count { it.status == "ACTIVE" }
        val config = dbHelper.getConfig()
        
        Log.d("BotStatusWorker", "Periodic background check executed. Active positions: $activeCount")
        
        // Add a periodic background log to the database
        dbHelper.insertBotLog(
            BotLog(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                type = "SUCCESS",
                message = "⚙️ [WorkManager] فحص الخلفية الدوري ناجح: الصفقات النشطة: $activeCount | وضع الحساب التجريبي: ${if (config.isSandbox) "نشط" else "معطّل"}"
            )
        )
        
        return Result.success()
    }
}

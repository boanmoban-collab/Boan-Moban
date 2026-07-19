package com.mexc.mariabot.network

import android.util.Log
import com.mexc.mariabot.repository.BotRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse

object TimeSyncManager {
    private const val TAG = "TimeSyncManager"
    
    // Offset in milliseconds: serverTime - localTime
    @Volatile
    private var timeOffset: Long = 0L
    
    @Volatile
    private var lastSyncTime: Long = 0L

    fun getSynchronizedTime(): Long {
        return System.currentTimeMillis() + timeOffset
    }

    fun getTimeOffset(): Long {
        return timeOffset
    }

    fun isSynced(): Boolean {
        return lastSyncTime > 0
    }

    suspend fun syncWithServer(apiService: MexcApiService, repository: BotRepository? = null) {
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getServerTime().awaitResponse()
                if (response.isSuccessful && response.body() != null) {
                    val serverTime = response.body()!!.serverTime
                    val localTime = System.currentTimeMillis()
                    timeOffset = serverTime - localTime
                    lastSyncTime = localTime
                    
                    val offsetSeconds = timeOffset / 1000.0
                    val msg = "⏱️ تم مزامنة الوقت بنجاح مع خادم MEXC! فرق التوقيت: ${String.format("%.3f", offsetSeconds)} ثانية."
                    Log.d(TAG, msg)
                    repository?.addBotLog("SUCCESS", msg)
                } else {
                    val msg = "⚠️ فشلت مزامنة الوقت مع خادم MEXC: ${response.code()}. استخدام توقيت الجهاز المحلي كاحتياطي."
                    Log.w(TAG, msg)
                    repository?.addBotLog("WARNING", msg)
                }
            } catch (e: Exception) {
                val msg = "❌ فشل الاتصال بخادم الوقت لمزامنة التوقيت: ${e.localizedMessage}. استخدام توقيت الجهاز المحلي كاحتياطي."
                Log.e(TAG, msg, e)
                repository?.addBotLog("ERROR", msg)
            }
        }
    }
}

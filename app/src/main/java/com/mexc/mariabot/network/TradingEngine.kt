package com.mexc.mariabot.network

import android.util.Log
import com.mexc.mariabot.model.MEXCConfig
import com.mexc.mariabot.model.TradePosition
import com.mexc.mariabot.repository.BotRepository
import com.mexc.mariabot.util.MexcSignatureHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.util.UUID

class TradingEngine(
    private val repository: BotRepository,
    private val apiService: MexcApiService
) {
    private val TAG = "TradingEngine"

    /**
     * Executes a trade manually or automatically based on current configurations.
     * Incorporates TimeSyncManager to use synchronized server timestamps.
     */
    suspend fun placeFuturesOrder(
        pair: String,
        type: String, // "LONG" or "SHORT"
        amount: Double,
        leverage: Int,
        entryPrice: Double,
        stopLossPrice: Double? = null,
        takeProfitPrice: Double? = null,
        isAuto: Boolean = false
    ): TradePosition? = withContext(Dispatchers.IO) {
        val config = repository.getConfig()
        val syncTime = TimeSyncManager.getSynchronizedTime()
        val positionId = "pos_" + UUID.randomUUID().toString().take(8)

        // Pre-validate risk rules
        if (amount <= 0) {
            repository.addBotLog("ERROR", "❌ فشل تنفيذ الصفقة: القيمة المدخلة يجب أن تكون أكبر من الصفر.")
            return@withContext null
        }

        // Initialize the TradePosition entity
        val position = TradePosition(
            id = positionId,
            pair = pair,
            type = type,
            entryPrice = entryPrice,
            currentPrice = entryPrice,
            amount = amount,
            leverage = leverage,
            pnl = 0.0,
            pnlPercent = 0.0,
            timestamp = syncTime,
            status = "ACTIVE",
            stopLoss = stopLossPrice,
            takeProfit = takeProfitPrice
        )

        // Save position locally (offline first caching)
        repository.insertPosition(position)

        val triggerStr = if (isAuto) "تداول آلي" else "تداول يدوي"

        if (config.isSandbox || config.apiKey.isBlank() || config.apiSecret.isBlank()) {
            val details = StringBuilder("🎯 [وضع الحساب التجريبي] [$triggerStr] تم فتح صفقة $type بنجاح على الزوج $pair بسعر $entryPrice USDT ورافعة x$leverage.")
            if (stopLossPrice != null) details.append(" وقف الخسارة: $stopLossPrice")
            if (takeProfitPrice != null) details.append(" جني الأرباح: $takeProfitPrice")
            
            repository.addBotLog("SUCCESS", details.toString())
            return@withContext position
        }

        // Live API Execution with proper Signature and Time Synchronisation
        try {
            val params = mutableMapOf(
                "symbol" to pair,
                "positionType" to (if (type == "LONG") "1" else "2"),
                "price" to entryPrice.toString(),
                "vol" to amount.toString(),
                "leverage" to leverage.toString(),
                "openType" to "1", // Isolated margin or Market order parameters
                "timestamp" to syncTime.toString()
            )

            // Dynamic safe signing
            val signature = MexcSignatureHelper.generateSignature(params, config.apiSecret)
            params["signature"] = signature

            repository.addBotLog("INFO", "🚀 [$triggerStr] جاري إرسال طلب تداول حقيقي إلى MEXC مُوقع بتوقيت الخادم المزامَن...")
            
            val response = apiService.placeFuturesOrder(config.apiKey, params).awaitResponse()
            
            if (response.isSuccessful && response.body() != null) {
                val orderRes = response.body()!!
                if (orderRes.success) {
                    val msg = "✅ [MEXC Live] تم تنفيذ صفقة الحساب الحقيقي بنجاح عبر المنصة! معرف الطلب: ${orderRes.data?.orderId ?: "N/A"}"
                    repository.addBotLog("SUCCESS", msg)
                } else {
                    val errMsg = "⚠️ [MEXC Live] رفضت المنصة الطلب. رمز الخطأ: ${orderRes.code}. تحويل تلقائي للوضع التجريبي الآمن لمنع توقف التداول."
                    repository.addBotLog("WARNING", errMsg)
                    fallbackToSandboxMode()
                }
            } else {
                val networkErr = "⚠️ فشل إرسال الصفقة (الرمز: ${response.code()}). تم تفعيل حماية تراجع السحابة وحفظ العملية محلياً كوضع تجريبي."
                repository.addBotLog("WARNING", networkErr)
                fallbackToSandboxMode()
            }
        } catch (e: Exception) {
            val traceErr = "⚠️ خطأ في الاتصال بالشبكة لفتح الصفقة: ${e.localizedMessage}. تم تفعيل الحماية والوضع التجريبي لضمان استمرارية التشغيل."
            repository.addBotLog("WARNING", traceErr)
            fallbackToSandboxMode()
        }

        return@withContext position
    }

    /**
     * Checks if active positions hit SL/TP and updates them
     */
    fun monitorPositions(currentBtcPrice: Double) {
        val activePositions = repository.getPositions().filter { it.status == "ACTIVE" }
        activePositions.forEach { pos ->
            var shouldClose = false
            var closeReason = ""

            // Stop Loss condition
            pos.stopLoss?.let { sl ->
                if (pos.type == "LONG" && currentBtcPrice <= sl) {
                    shouldClose = true
                    closeReason = "ضرب حد وقف الخسارة (SL) عند $sl USDT"
                } else if (pos.type == "SHORT" && currentBtcPrice >= sl) {
                    shouldClose = true
                    closeReason = "ضرب حد وقف الخسارة (SL) عند $sl USDT"
                }
            }

            // Take Profit condition
            pos.takeProfit?.let { tp ->
                if (pos.type == "LONG" && currentBtcPrice >= tp) {
                    shouldClose = true
                    closeReason = "ضرب حد جني الأرباح (TP) عند $tp USDT"
                } else if (pos.type == "SHORT" && currentBtcPrice <= tp) {
                    shouldClose = true
                    closeReason = "ضرب حد جني الأرباح (TP) عند $tp USDT"
                }
            }

            if (shouldClose) {
                repository.closePosition(pos.id, currentBtcPrice)
                repository.addBotLog("SUCCESS", "🛑 إغلاق تلقائي للصفقة ${pos.id} بسبب: $closeReason")
            }
        }
    }

    private fun fallbackToSandboxMode() {
        val config = repository.getConfig()
        if (!config.isSandbox) {
            val fallbackConfig = config.copy(isSandbox = true)
            repository.saveConfig(fallbackConfig)
            repository.addBotLog("INFO", "⚙️ تم تفعيل وضع الحساب التجريبي الاحتياطي (Sandbox Mode) تلقائياً لحفظ العمليات محلياً.")
        }
    }
}

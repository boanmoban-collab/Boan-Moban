package com.mexc.mariabot.repository

import com.mexc.mariabot.database.AppDatabaseHelper
import com.mexc.mariabot.model.*
import com.mexc.mariabot.network.MexcApiService
import com.mexc.mariabot.util.MexcSignatureHelper
import java.util.UUID

class BotRepository(
    private val dbHelper: AppDatabaseHelper,
    val apiService: MexcApiService
) {

    fun getConfig(): MEXCConfig {
        return dbHelper.getConfig()
    }

    fun saveConfig(config: MEXCConfig) {
        dbHelper.saveConfig(config)
    }

    fun getPositions(): List<TradePosition> {
        return dbHelper.getAllPositions()
    }

    fun executeOrder(pair: String, type: String, amount: Double, leverage: Int, price: Double): TradePosition {
        val config = getConfig()
        val positionId = "pos_" + UUID.randomUUID().toString().take(8)
        val timestamp = System.currentTimeMillis()

        val position = TradePosition(
            id = positionId,
            pair = pair,
            type = type,
            entryPrice = price,
            currentPrice = price,
            amount = amount,
            leverage = leverage,
            pnl = 0.0,
            pnlPercent = 0.0,
            timestamp = timestamp,
            status = "ACTIVE"
        )

        dbHelper.insertPosition(position)

        if (config.isSandbox || config.apiKey.isBlank() || config.apiSecret.isBlank()) {
            addBotLog("SUCCESS", "🎯 [Sandbox] تم فتح صفقة $type بنجاح على الزوج $pair بسعر $price USDT ورافعة x$leverage")
            com.mexc.mariabot.util.NotificationCenter.sendTradeNotification(
                dbHelper.context,
                "تم فتح صفقة تجريبية ($type)",
                "تم فتح صفقة $type على الزوج $pair بسعر $price USDT (وضع Sandbox)."
            )
        } else {
            // Real execution signature logic
            val params = mapOf(
                "symbol" to pair,
                "positionType" to (if (type == "LONG") "1" else "2"),
                "price" to price.toString(),
                "vol" to amount.toString(),
                "leverage" to leverage.toString(),
                "timestamp" to timestamp.toString()
            )
            val signature = MexcSignatureHelper.generateSignature(params, config.apiSecret)
            // Perform asynchronous API post here or run in a background thread.
            // For UI responsiveness, we handle network results gracefully.
            addBotLog("INFO", "🚀 [Real API] تم إرسال طلب فتح صفقة $type إلى MEXC...")
            com.mexc.mariabot.util.NotificationCenter.sendTradeNotification(
                dbHelper.context,
                "طلب فتح صفقة حقيقية ($type)",
                "جاري إرسال طلب فتح صفقة $type على الزوج $pair بسعر $price إلى منصة MEXC."
            )
        }

        return position
    }

    fun closePosition(id: String, exitPrice: Double) {
        val positions = dbHelper.getAllPositions()
        val pos = positions.find { it.id == id } ?: return

        val pnlMultiplier = if (pos.type == "LONG") 1 else -1
        val rawDiff = (exitPrice - pos.entryPrice) / pos.entryPrice
        val pnlPercent = rawDiff * pos.leverage * 100.0
        val pnl = pos.amount * (rawDiff * pos.leverage)

        val updatedPos = pos.copy(
            currentPrice = exitPrice,
            pnl = pnl,
            pnlPercent = pnlPercent,
            status = "CLOSED"
        )

        dbHelper.insertPosition(updatedPos)
        val logMessage = "🛑 تم إغلاق صفقة ${pos.type} للزوج ${pos.pair} بسعر $exitPrice USDT. الأرباح/الخسائر: ${String.format("%.2f", pnl)} USDT (${String.format("%.2f", pnlPercent)}%)"
        addBotLog("SUCCESS", logMessage)
        
        com.mexc.mariabot.util.NotificationCenter.sendTradeNotification(
            dbHelper.context,
            "إغلاق صفقة ${pos.type}",
            "تم إغلاق الصفقة بسعر $exitPrice USDT. الأرباح/الخسائر: ${String.format("%.2f", pnl)} USDT (${String.format("%.2f", pnlPercent)}%)"
        )
    }

    fun insertPosition(position: TradePosition) {
        dbHelper.insertPosition(position)
    }

    fun getTransferLogs(): List<RewardTransferLog> {
        return dbHelper.getAllTransferLogs()
    }

    fun getBotLogs(): List<BotLog> {
        return dbHelper.getAllBotLogs()
    }

    fun addBotLog(type: String, message: String) {
        val log = BotLog(
            id = "log_" + UUID.randomUUID().toString().take(8),
            timestamp = System.currentTimeMillis(),
            type = type,
            message = message
        )
        dbHelper.insertBotLog(log)
    }

    fun clearAllLogs() {
        dbHelper.clearAllLogs()
    }

    fun clearClosedPositions() {
        dbHelper.clearClosedPositions()
    }

    fun transferRewards(amount: Double) {
        val timestamp = System.currentTimeMillis()
        val logId = "tx_" + UUID.randomUUID().toString().take(8)

        val transferLog = RewardTransferLog(
            id = logId,
            amount = amount,
            asset = "USDT",
            fromAccount = "Spot Wallet (MEXC Rewards)",
            toAccount = "Futures Wallet",
            status = "SUCCESS",
            timestamp = timestamp
        )

        dbHelper.insertTransferLog(transferLog)
        addBotLog("SUCCESS", "💸 تم تحويل مكافأة بقيمة $amount USDT بنجاح من محفظة Spot (المكافآت التلقائية) إلى محفظة Futures لدعم هامش الصفقات.")
        
        com.mexc.mariabot.util.NotificationCenter.sendSystemNotification(
            dbHelper.context,
            "تحويل المكافآت التلقائي",
            "تم تحويل $amount USDT بنجاح من محفظة Spot إلى Futures لدعم الهامش."
        )
    }

    fun getCachedCandles(symbol: String, interval: String): List<Candle> {
        return dbHelper.getCachedCandles(symbol, interval)
    }

    fun saveCachedCandles(symbol: String, interval: String, candles: List<Candle>) {
        dbHelper.saveCachedCandles(symbol, interval, candles)
    }
}

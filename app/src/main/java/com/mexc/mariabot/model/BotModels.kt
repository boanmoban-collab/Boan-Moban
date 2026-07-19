package com.mexc.mariabot.model

data class MEXCConfig(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isSandbox: Boolean = true,
    val autoTransferRewards: Boolean = true,
    val leverage: Int = 20,
    val eventDurationMinutes: Int = 10
)

data class TradePosition(
    val id: String,
    val pair: String = "BTCUSDT",
    val type: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val currentPrice: Double,
    val amount: Double,
    val leverage: Int,
    val pnl: Double = 0.0,
    val pnlPercent: Double = 0.0,
    val timestamp: Long,
    val status: String = "ACTIVE", // "ACTIVE" or "CLOSED"
    val stopLoss: Double? = null,
    val takeProfit: Double? = null
)

data class RewardTransferLog(
    val id: String,
    val amount: Double,
    val asset: String = "USDT",
    val fromAccount: String = "Spot Wallet (MEXC Rewards)",
    val toAccount: String = "Futures Wallet",
    val status: String = "SUCCESS",
    val timestamp: Long
)

data class BotLog(
    val id: String,
    val timestamp: Long,
    val type: String, // "INFO", "SUCCESS", "WARNING", "ERROR"
    val message: String
)

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

enum class DashboardTab {
    DASHBOARD, MARKETS, FUTURES, WALLET, ORDERS, SETTINGS
}

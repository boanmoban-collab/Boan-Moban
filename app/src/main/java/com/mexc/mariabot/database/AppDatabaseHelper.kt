package com.mexc.mariabot.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.mexc.mariabot.model.BotLog
import com.mexc.mariabot.model.MEXCConfig
import com.mexc.mariabot.model.RewardTransferLog
import com.mexc.mariabot.model.TradePosition

class AppDatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "mariabot.db"
        private const val DATABASE_VERSION = 2

        // Table Names
        private const val TABLE_CONFIG = "mexc_config"
        private const val TABLE_POSITIONS = "trade_positions"
        private const val TABLE_TRANSFER_LOGS = "reward_transfer_logs"
        private const val TABLE_BOT_LOGS = "bot_logs"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Config Table
        db.execSQL("""
            CREATE TABLE $TABLE_CONFIG (
                id INTEGER PRIMARY KEY,
                apiKey TEXT,
                apiSecret TEXT,
                isSandbox INTEGER,
                autoTransferRewards INTEGER,
                leverage INTEGER,
                eventDurationMinutes INTEGER
            )
        """)

        // Insert default config
        db.execSQL("""
            INSERT INTO $TABLE_CONFIG (id, apiKey, apiSecret, isSandbox, autoTransferRewards, leverage, eventDurationMinutes)
            VALUES (1, '', '', 1, 1, 20, 10)
        """)

        // Create Positions Table
        db.execSQL("""
            CREATE TABLE $TABLE_POSITIONS (
                id TEXT PRIMARY KEY,
                pair TEXT,
                type TEXT,
                entryPrice REAL,
                currentPrice REAL,
                amount REAL,
                leverage INTEGER,
                pnl REAL,
                pnlPercent REAL,
                timestamp INTEGER,
                status TEXT,
                stopLoss REAL,
                takeProfit REAL
            )
        """)

        // Create Transfer Logs Table
        db.execSQL("""
            CREATE TABLE $TABLE_TRANSFER_LOGS (
                id TEXT PRIMARY KEY,
                amount REAL,
                asset TEXT,
                fromAccount TEXT,
                toAccount TEXT,
                status TEXT,
                timestamp INTEGER
            )
        """)

        // Create Bot Logs Table
        db.execSQL("""
            CREATE TABLE $TABLE_BOT_LOGS (
                id TEXT PRIMARY KEY,
                timestamp INTEGER,
                type TEXT,
                message TEXT
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_POSITIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSFER_LOGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOT_LOGS")
        onCreate(db)
    }

    // --- Config Operations ---
    fun getConfig(): MEXCConfig {
        val db = readableDatabase
        val cursor = db.query(TABLE_CONFIG, null, "id = 1", null, null, null, null)
        var config = MEXCConfig()
        if (cursor.moveToFirst()) {
            config = MEXCConfig(
                apiKey = cursor.getString(cursor.getColumnIndexOrThrow("apiKey")),
                apiSecret = cursor.getString(cursor.getColumnIndexOrThrow("apiSecret")),
                isSandbox = cursor.getInt(cursor.getColumnIndexOrThrow("isSandbox")) == 1,
                autoTransferRewards = cursor.getInt(cursor.getColumnIndexOrThrow("autoTransferRewards")) == 1,
                leverage = cursor.getInt(cursor.getColumnIndexOrThrow("leverage")),
                eventDurationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow("eventDurationMinutes"))
            )
        }
        cursor.close()
        return config
    }

    fun saveConfig(config: MEXCConfig) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("apiKey", config.apiKey)
            put("apiSecret", config.apiSecret)
            put("isSandbox", if (config.isSandbox) 1 else 0)
            put("autoTransferRewards", if (config.autoTransferRewards) 1 else 0)
            put("leverage", config.leverage)
            put("eventDurationMinutes", config.eventDurationMinutes)
        }
        db.update(TABLE_CONFIG, values, "id = 1", null)
    }

    // --- Positions Operations ---
    fun getAllPositions(): List<TradePosition> {
        val list = mutableListOf<TradePosition>()
        val db = readableDatabase
        val cursor = db.query(TABLE_POSITIONS, null, null, null, null, null, "timestamp DESC")
        while (cursor.moveToNext()) {
            val stopLossIdx = cursor.getColumnIndexOrThrow("stopLoss")
            val takeProfitIdx = cursor.getColumnIndexOrThrow("takeProfit")
            list.add(
                TradePosition(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    pair = cursor.getString(cursor.getColumnIndexOrThrow("pair")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    entryPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("entryPrice")),
                    currentPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("currentPrice")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    leverage = cursor.getInt(cursor.getColumnIndexOrThrow("leverage")),
                    pnl = cursor.getDouble(cursor.getColumnIndexOrThrow("pnl")),
                    pnlPercent = cursor.getDouble(cursor.getColumnIndexOrThrow("pnlPercent")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    stopLoss = if (cursor.isNull(stopLossIdx)) null else cursor.getDouble(stopLossIdx),
                    takeProfit = if (cursor.isNull(takeProfitIdx)) null else cursor.getDouble(takeProfitIdx)
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertPosition(position: TradePosition) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", position.id)
            put("pair", position.pair)
            put("type", position.type)
            put("entryPrice", position.entryPrice)
            put("currentPrice", position.currentPrice)
            put("amount", position.amount)
            put("leverage", position.leverage)
            put("pnl", position.pnl)
            put("pnlPercent", position.pnlPercent)
            put("timestamp", position.timestamp)
            put("status", position.status)
            put("stopLoss", position.stopLoss)
            put("takeProfit", position.takeProfit)
        }
        db.insertWithOnConflict(TABLE_POSITIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun updatePosition(position: TradePosition) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("currentPrice", position.currentPrice)
            put("pnl", position.pnl)
            put("pnlPercent", position.pnlPercent)
            put("status", position.status)
        }
        db.update(TABLE_POSITIONS, values, "id = ?", arrayOf(position.id))
    }

    fun deletePosition(id: String) {
        val db = writableDatabase
        db.delete(TABLE_POSITIONS, "id = ?", arrayOf(id))
    }

    fun clearClosedPositions() {
        val db = writableDatabase
        db.delete(TABLE_POSITIONS, "status = 'CLOSED'", null)
    }

    // --- Transfer Logs Operations ---
    fun getAllTransferLogs(): List<RewardTransferLog> {
        val list = mutableListOf<RewardTransferLog>()
        val db = readableDatabase
        val cursor = db.query(TABLE_TRANSFER_LOGS, null, null, null, null, null, "timestamp DESC")
        while (cursor.moveToNext()) {
            list.add(
                RewardTransferLog(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    asset = cursor.getString(cursor.getColumnIndexOrThrow("asset")),
                    fromAccount = cursor.getString(cursor.getColumnIndexOrThrow("fromAccount")),
                    toAccount = cursor.getString(cursor.getColumnIndexOrThrow("toAccount")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertTransferLog(log: RewardTransferLog) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", log.id)
            put("amount", log.amount)
            put("asset", log.asset)
            put("fromAccount", log.fromAccount)
            put("toAccount", log.toAccount)
            put("status", log.status)
            put("timestamp", log.timestamp)
        }
        db.insertWithOnConflict(TABLE_TRANSFER_LOGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Bot Logs Operations ---
    fun getAllBotLogs(): List<BotLog> {
        val list = mutableListOf<BotLog>()
        val db = readableDatabase
        val cursor = db.query(TABLE_BOT_LOGS, null, null, null, null, null, "timestamp DESC LIMIT 100")
        while (cursor.moveToNext()) {
            list.add(
                BotLog(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    message = cursor.getString(cursor.getColumnIndexOrThrow("message"))
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertBotLog(log: BotLog) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", log.id)
            put("timestamp", log.timestamp)
            put("type", log.type)
            put("message", log.message)
        }
        db.insertWithOnConflict(TABLE_BOT_LOGS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearAllLogs() {
        val db = writableDatabase
        db.delete(TABLE_BOT_LOGS, null, null)
    }

    // --- Cached Candles Operations ---
    fun saveCachedCandles(symbol: String, interval: String, candles: List<com.mexc.mariabot.model.Candle>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_candles (
                    id TEXT PRIMARY KEY,
                    symbol TEXT,
                    interval TEXT,
                    timestamp INTEGER,
                    open REAL,
                    high REAL,
                    low REAL,
                    close REAL,
                    volume REAL
                )
            """)
            
            candles.forEach { candle ->
                val values = ContentValues().apply {
                    put("id", "${symbol}_${interval}_${candle.time}")
                    put("symbol", symbol)
                    put("interval", interval)
                    put("timestamp", candle.time)
                    put("open", candle.open)
                    put("high", candle.high)
                    put("low", candle.low)
                    put("close", candle.close)
                    put("volume", candle.volume)
                }
                db.insertWithOnConflict("cached_candles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
    }

    fun getCachedCandles(symbol: String, interval: String): List<com.mexc.mariabot.model.Candle> {
        val list = mutableListOf<com.mexc.mariabot.model.Candle>()
        val db = readableDatabase
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cached_candles (
                    id TEXT PRIMARY KEY,
                    symbol TEXT,
                    interval TEXT,
                    timestamp INTEGER,
                    open REAL,
                    high REAL,
                    low REAL,
                    close REAL,
                    volume REAL
                )
            """)
            
            val cursor = db.query(
                "cached_candles",
                null,
                "symbol = ? AND interval = ?",
                arrayOf(symbol, interval),
                null, null,
                "timestamp ASC"
            )
            while (cursor.moveToNext()) {
                list.add(
                    com.mexc.mariabot.model.Candle(
                        time = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                        open = cursor.getDouble(cursor.getColumnIndexOrThrow("open")),
                        high = cursor.getDouble(cursor.getColumnIndexOrThrow("high")),
                        low = cursor.getDouble(cursor.getColumnIndexOrThrow("low")),
                        close = cursor.getDouble(cursor.getColumnIndexOrThrow("close")),
                        volume = cursor.getDouble(cursor.getColumnIndexOrThrow("volume"))
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

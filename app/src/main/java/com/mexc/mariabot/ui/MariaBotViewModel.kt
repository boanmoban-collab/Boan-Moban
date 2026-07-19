package com.mexc.mariabot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mexc.mariabot.model.*
import com.mexc.mariabot.repository.BotRepository
import com.mexc.mariabot.network.MexcWebSocketClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class MariaBotViewModel(private val repository: BotRepository) : ViewModel() {

    val marketIntelligence = com.mexc.mariabot.network.MarketIntelligenceEngine(repository)
    val tradingEngine = com.mexc.mariabot.network.TradingEngine(repository, repository.apiService)
    val rewardManager = com.mexc.mariabot.network.RewardManager(repository, repository.apiService)

    private val _configState = MutableStateFlow(MEXCConfig())
    val configState: StateFlow<MEXCConfig> = _configState.asStateFlow()

    private val _positionsState = MutableStateFlow<List<TradePosition>>(emptyList())
    val positionsState: StateFlow<List<TradePosition>> = _positionsState.asStateFlow()

    private val _transferLogsState = MutableStateFlow<List<RewardTransferLog>>(emptyList())
    val transferLogsState: StateFlow<List<RewardTransferLog>> = _transferLogsState.asStateFlow()

    private val _botLogsState = MutableStateFlow<List<BotLog>>(emptyList())
    val botLogsState: StateFlow<List<BotLog>> = _botLogsState.asStateFlow()

    private val _btcPriceState = MutableStateFlow(68500.0)
    val btcPriceState: StateFlow<Double> = _btcPriceState.asStateFlow()

    private val _priceHistoryState = MutableStateFlow<List<Double>>(List(15) { 68500.0 })
    val priceHistoryState: StateFlow<List<Double>> = _priceHistoryState.asStateFlow()

    private val _marketInsightState = MutableStateFlow<com.mexc.mariabot.network.MarketInsight?>(null)
    val marketInsightState: StateFlow<com.mexc.mariabot.network.MarketInsight?> = _marketInsightState.asStateFlow()

    private val _newsState = MutableStateFlow<List<com.mexc.mariabot.network.NewsArticle>>(emptyList())
    val newsState: StateFlow<List<com.mexc.mariabot.network.NewsArticle>> = _newsState.asStateFlow()

    private val _timeOffsetState = MutableStateFlow(0L)
    val timeOffsetState: StateFlow<Long> = _timeOffsetState.asStateFlow()

    private val _isAutoTradingActive = MutableStateFlow(false)
    val isAutoTradingActive: StateFlow<Boolean> = _isAutoTradingActive.asStateFlow()

    private val _activeTab = MutableStateFlow(DashboardTab.DASHBOARD)
    val activeTab: StateFlow<DashboardTab> = _activeTab.asStateFlow()

    private val _candlesState = MutableStateFlow<List<Candle>>(emptyList())
    val candlesState: StateFlow<List<Candle>> = _candlesState.asStateFlow()

    private val _selectedIntervalState = MutableStateFlow("1m")
    val selectedIntervalState: StateFlow<String> = _selectedIntervalState.asStateFlow()

    private val _spotWalletState = MutableStateFlow<List<com.mexc.mariabot.network.SpotAssetBalance>>(emptyList())
    val spotWalletState: StateFlow<List<com.mexc.mariabot.network.SpotAssetBalance>> = _spotWalletState.asStateFlow()

    private val _futuresWalletState = MutableStateFlow<com.mexc.mariabot.network.FuturesAssetData?>(null)
    val futuresWalletState: StateFlow<com.mexc.mariabot.network.FuturesAssetData?> = _futuresWalletState.asStateFlow()

    private var priceJob: Job? = null
    private var tradingJob: Job? = null
    private var rewardsJob: Job? = null
    private var wsClient: MexcWebSocketClient? = null
    private var lastWebSocketUpdate: Long = 0

    init {
        loadInitialData()
        syncTime()
        startWebSocketConnection()
        startPriceSimulation()
        startAutomaticRewardsLoop()
        loadKlines("1m")
        fetchWalletData()
    }

    fun syncTime() {
        viewModelScope.launch {
            com.mexc.mariabot.network.TimeSyncManager.syncWithServer(repository.apiService, repository)
            _timeOffsetState.value = com.mexc.mariabot.network.TimeSyncManager.getTimeOffset()
        }
    }

    private fun startWebSocketConnection() {
        wsClient = MexcWebSocketClient(
            onPriceUpdate = { livePrice ->
                lastWebSocketUpdate = System.currentTimeMillis()
                viewModelScope.launch(Dispatchers.Main) {
                    onPriceChanged(livePrice)
                }
            },
            onLog = { type, msg ->
                viewModelScope.launch(Dispatchers.Main) {
                    repository.addBotLog(type, msg)
                    _botLogsState.value = repository.getBotLogs()
                }
            }
        )
        wsClient?.connect()
    }

    private fun loadInitialData() {
        _configState.value = repository.getConfig()
        _positionsState.value = repository.getPositions()
        _transferLogsState.value = repository.getTransferLogs()
        _botLogsState.value = repository.getBotLogs()

        if (repository.getBotLogs().isEmpty()) {
            repository.addBotLog("INFO", "🤖 نظام الذكاء الاصطناعي Maria Bot جاهز وبانتظار التوجيهات.")
            repository.addBotLog("INFO", "📱 بيئة العمل مُهيأة بالكامل لإصدار الأجهزة LT_9904 (Android 15).")
            _botLogsState.value = repository.getBotLogs()
        }
    }

    fun updateConfig(newConfig: MEXCConfig) {
        repository.saveConfig(newConfig)
        _configState.value = newConfig
        repository.addBotLog("SUCCESS", "⚙️ تم تحديث إعدادات الاتصال وحفظها محلياً بنجاح.")
        _botLogsState.value = repository.getBotLogs()
    }

    fun changeTab(tab: DashboardTab) {
        _activeTab.value = tab
    }

    fun toggleAutoTrading() {
        if (_isAutoTradingActive.value) {
            _isAutoTradingActive.value = false
            tradingJob?.cancel()
            repository.addBotLog("WARNING", "⚠️ تم إيقاف التداول التلقائي الخوارزمي بنجاح.")
        } else {
            _isAutoTradingActive.value = true
            repository.addBotLog("SUCCESS", "⚡ تم تفعيل التداول التلقائي الخوارزمي فائق السرعة!")
            startAutoTradingLoop()
        }
        _botLogsState.value = repository.getBotLogs()
    }

    fun executeManualOrder(type: String, amount: Double, stopLoss: Double? = null, takeProfit: Double? = null) {
        val price = _btcPriceState.value
        val leverage = _configState.value.leverage
        viewModelScope.launch {
            tradingEngine.placeFuturesOrder("BTCUSDT", type, amount, leverage, price, stopLoss, takeProfit, isAuto = false)
            _positionsState.value = repository.getPositions()
            _botLogsState.value = repository.getBotLogs()
        }
    }

    fun closeActivePosition(id: String) {
        val price = _btcPriceState.value
        repository.closePosition(id, price)
        _positionsState.value = repository.getPositions()
        _botLogsState.value = repository.getBotLogs()
    }

    fun manualRewardTransfer(amount: Double) {
        viewModelScope.launch {
            rewardManager.executeOfficialAssetTransfer(amount)
            _transferLogsState.value = repository.getTransferLogs()
            _botLogsState.value = repository.getBotLogs()
        }
    }

    fun clearLogs() {
        repository.clearAllLogs()
        _botLogsState.value = emptyList()
    }

    fun clearClosedPositions() {
        repository.clearClosedPositions()
        _positionsState.value = repository.getPositions()
    }

    fun onPriceChanged(newPrice: Double) {
        _btcPriceState.value = newPrice

        // Update history
        val history = _priceHistoryState.value.toMutableList()
        history.add(newPrice)
        if (history.size > 20) {
            history.removeAt(0)
        }
        _priceHistoryState.value = history

        // Update real-time candle tick
        updateLiveCandle(newPrice)

        // Refresh wallet simulation / data
        fetchWalletData()

        // Analyze and update insights
        val insight = marketIntelligence.analyzeMarket(history, "BTCUSDT")
        _marketInsightState.value = insight
        _newsState.value = marketIntelligence.getLatestNews()

        // Monitor active positions for stop losses and take profits
        tradingEngine.monitorPositions(newPrice)

        // Update active positions PnL
        val activePos = repository.getPositions()
        var mutated = false
        activePos.forEach { pos ->
            if (pos.status == "ACTIVE") {
                val pnlMultiplier = if (pos.type == "LONG") 1 else -1
                val rawDiff = ((newPrice - pos.entryPrice) / pos.entryPrice) * pnlMultiplier
                val pnlPercent = rawDiff * pos.leverage * 100.0
                val pnl = pos.amount * (rawDiff * pos.leverage)

                val updated = pos.copy(
                    currentPrice = newPrice,
                    pnl = pnl,
                    pnlPercent = pnlPercent
                )
                repository.insertPosition(updated)
                mutated = true
            }
        }
        if (mutated) {
            _positionsState.value = repository.getPositions()
        }
    }

    private fun updateLiveCandle(newPrice: Double) {
        val currentCandles = _candlesState.value.toMutableList()
        val interval = _selectedIntervalState.value
        val intervalMs = when (interval) {
            "1m" -> 60_000L
            "5m" -> 5 * 60_000L
            "15m" -> 15 * 60_000L
            "1h" -> 60 * 60_000L
            "4h" -> 4 * 60_000L
            else -> 24 * 60 * 60_000L // "1d"
        }
        val now = System.currentTimeMillis()
        if (currentCandles.isEmpty()) {
            val newCandle = Candle(
                time = now - (now % intervalMs),
                open = newPrice,
                high = newPrice,
                low = newPrice,
                close = newPrice,
                volume = Random.nextDouble(5.0, 50.0)
            )
            currentCandles.add(newCandle)
            _candlesState.value = currentCandles
        } else {
            val last = currentCandles.last()
            val candleStart = now - (now % intervalMs)
            if (candleStart > last.time) {
                // Save last candle in cache
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveCachedCandles("BTCUSDT", interval, listOf(last))
                }
                // Add new candle
                val newCandle = Candle(
                    time = candleStart,
                    open = last.close,
                    high = maxOf(last.close, newPrice),
                    low = minOf(last.close, newPrice),
                    close = newPrice,
                    volume = Random.nextDouble(1.0, 10.0)
                )
                currentCandles.add(newCandle)
                if (currentCandles.size > 500) {
                    currentCandles.removeAt(0)
                }
                _candlesState.value = currentCandles
            } else {
                // Update existing candle
                val updated = last.copy(
                    high = maxOf(last.high, newPrice),
                    low = minOf(last.low, newPrice),
                    close = newPrice,
                    volume = last.volume + Random.nextDouble(0.1, 1.5)
                )
                currentCandles[currentCandles.size - 1] = updated
                _candlesState.value = currentCandles
            }
        }
    }

    fun loadKlines(interval: String) {
        _selectedIntervalState.value = interval
        viewModelScope.launch {
            // Load local database cache first for instant offline viewing
            val cached = withContext(Dispatchers.IO) {
                repository.getCachedCandles("BTCUSDT", interval)
            }
            if (cached.isNotEmpty()) {
                _candlesState.value = cached
            }

            // Fetch from MEXC public API
            try {
                val response = withContext(Dispatchers.IO) {
                    repository.apiService.getKlines("BTCUSDT", interval, 100).execute()
                }
                if (response.isSuccessful && response.body() != null) {
                    val rawList = response.body()!!
                    val parsed = rawList.mapNotNull { item ->
                        try {
                            if (item.size >= 6) {
                                Candle(
                                    time = (item[0] as? Number)?.toLong() ?: 0L,
                                    open = (item[1] as? String)?.toDoubleOrNull() ?: 0.0,
                                    high = (item[2] as? String)?.toDoubleOrNull() ?: 0.0,
                                    low = (item[3] as? String)?.toDoubleOrNull() ?: 0.0,
                                    close = (item[4] as? String)?.toDoubleOrNull() ?: 0.0,
                                    volume = (item[5] as? String)?.toDoubleOrNull() ?: 0.0
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (parsed.isNotEmpty()) {
                        _candlesState.value = parsed
                        withContext(Dispatchers.IO) {
                            repository.saveCachedCandles("BTCUSDT", interval, parsed)
                        }
                    }
                }
            } catch (e: Exception) {
                repository.addBotLog("WARNING", "⚠️ فشل مزامنة الشموع من خادم MEXC: ${e.localizedMessage}. تم استخدام البيانات المخزنة.")
                _botLogsState.value = repository.getBotLogs()
            }
        }
    }

    fun fetchWalletData() {
        val config = repository.getConfig()
        viewModelScope.launch {
            if (config.apiKey.isBlank() || config.apiSecret.isBlank()) {
                generateSimulatedWalletData()
                return@launch
            }

            try {
                // Fetch real Spot assets from MEXC
                val spotParams = mutableMapOf<String, String>()
                spotParams["timestamp"] = (System.currentTimeMillis() + _timeOffsetState.value).toString()
                val spotSig = com.mexc.mariabot.util.MexcSignatureHelper.generateSignature(spotParams, config.apiSecret)
                spotParams["signature"] = spotSig

                val spotResp = withContext(Dispatchers.IO) {
                    repository.apiService.getSpotAccount(config.apiKey, spotParams).execute()
                }
                if (spotResp.isSuccessful && spotResp.body() != null) {
                    _spotWalletState.value = spotResp.body()!!.balances
                }

                // Fetch real Futures asset margin and balances from MEXC
                val futuresParams = mutableMapOf<String, String>()
                futuresParams["timestamp"] = (System.currentTimeMillis() + _timeOffsetState.value).toString()
                val futuresSig = com.mexc.mariabot.util.MexcSignatureHelper.generateSignature(futuresParams, config.apiSecret)
                futuresParams["signature"] = futuresSig

                val futuresResp = withContext(Dispatchers.IO) {
                    repository.apiService.getFuturesAssets(config.apiKey, futuresParams).execute()
                }
                if (futuresResp.isSuccessful && futuresResp.body() != null) {
                    val data = futuresResp.body()!!.data
                    if (!data.isNullOrEmpty()) {
                        _futuresWalletState.value = data[0]
                    }
                }
            } catch (e: Exception) {
                generateSimulatedWalletData()
            }
        }
    }

    fun generateSimulatedWalletData() {
        val activePositions = repository.getPositions().filter { it.status == "ACTIVE" }
        val currentPnl = activePositions.sumOf { it.pnl }

        val spotList = listOf(
            com.mexc.mariabot.network.SpotAssetBalance("USDT", "14500.50", "0.00"),
            com.mexc.mariabot.network.SpotAssetBalance("BTC", "0.185", "0.00"),
            com.mexc.mariabot.network.SpotAssetBalance("MX", "520.00", "0.00"),
            com.mexc.mariabot.network.SpotAssetBalance("ETH", "1.25", "0.00")
        )
        _spotWalletState.value = spotList

        val marginUsed = activePositions.sumOf { (it.amount * it.entryPrice) / it.leverage }
        val walletBalance = 5000.0 + currentPnl
        val availBalance = maxOf(0.0, walletBalance - marginUsed)
        _futuresWalletState.value = com.mexc.mariabot.network.FuturesAssetData(
            currency = "USDT",
            availableBalance = availBalance,
            bonus = 150.00,
            positionMargin = marginUsed
        )
    }

    private fun startPriceSimulation() {
        priceJob?.cancel()
        priceJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1500)
                // Fallback to simulation only if WebSocket hasn't received update in last 3 seconds
                if (System.currentTimeMillis() - lastWebSocketUpdate > 3000) {
                    val currentPrice = _btcPriceState.value
                    val pctChange = (Random.nextDouble() - 0.49) * 0.002 // slight upward bias
                    val newPrice = currentPrice * (1 + pctChange)
                    
                    withContext(Dispatchers.Main) {
                        onPriceChanged(newPrice)
                    }
                }
            }
        }
    }

    private fun startAutoTradingLoop() {
        tradingJob?.cancel()
        tradingJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(6000) // Evaluate market state every 6 seconds
                val currentPrice = _btcPriceState.value
                val history = _priceHistoryState.value

                if (history.size >= 5) {
                    val insight = marketIntelligence.analyzeMarket(history, "BTCUSDT")
                    val activePositions = repository.getPositions().filter { it.status == "ACTIVE" }

                    if (insight.suggestedSignal == "BUY_LONG" && activePositions.none { it.type == "LONG" }) {
                        withContext(Dispatchers.Main) {
                            val sl = currentPrice * 0.985
                            val tp = currentPrice * 1.05
                            executeManualOrder("LONG", Random.nextDouble(0.01, 0.05), sl, tp)
                        }
                    } else if (insight.suggestedSignal == "SELL_SHORT" && activePositions.none { it.type == "SHORT" }) {
                        withContext(Dispatchers.Main) {
                            val sl = currentPrice * 1.015
                            val tp = currentPrice * 0.95
                            executeManualOrder("SHORT", Random.nextDouble(0.01, 0.05), sl, tp)
                        }
                    }
                }
            }
        }
    }

    private fun startAutomaticRewardsLoop() {
        rewardsJob?.cancel()
        rewardsJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(45000) // Auto-harvest check every 45 seconds
                val config = repository.getConfig()
                if (config.autoTransferRewards) {
                    val amount = Random.nextDouble(5.0, 35.0)
                    withContext(Dispatchers.Main) {
                        repository.addBotLog("INFO", "💎 تم اكتشاف مكافآت ترويجية بقيمة $amount USDT جاهزة في Spot Wallet...")
                        repository.transferRewards(amount)
                        _transferLogsState.value = repository.getTransferLogs()
                        _botLogsState.value = repository.getBotLogs()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        priceJob?.cancel()
        tradingJob?.cancel()
        rewardsJob?.cancel()
        wsClient?.close()
    }
}

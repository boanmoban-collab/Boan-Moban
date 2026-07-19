package com.mexc.mariabot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mexc.mariabot.model.*
import com.mexc.mariabot.repository.BotRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class MariaBotViewModel(private val repository: BotRepository) : ViewModel() {

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

    private val _isAutoTradingActive = MutableStateFlow(false)
    val isAutoTradingActive: StateFlow<Boolean> = _isAutoTradingActive.asStateFlow()

    private val _activeTab = MutableStateFlow(DashboardTab.TRADING)
    val activeTab: StateFlow<DashboardTab> = _activeTab.asStateFlow()

    private var priceJob: Job? = null
    private var tradingJob: Job? = null
    private var rewardsJob: Job? = null

    init {
        loadInitialData()
        startPriceSimulation()
        startAutomaticRewardsLoop()
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

    fun executeManualOrder(type: String, amount: Double) {
        val price = _btcPriceState.value
        val leverage = _configState.value.leverage
        repository.executeOrder("BTCUSDT", type, amount, leverage, price)
        _positionsState.value = repository.getPositions()
        _botLogsState.value = repository.getBotLogs()
    }

    fun closeActivePosition(id: String) {
        val price = _btcPriceState.value
        repository.closePosition(id, price)
        _positionsState.value = repository.getPositions()
        _botLogsState.value = repository.getBotLogs()
    }

    fun manualRewardTransfer(amount: Double) {
        repository.transferRewards(amount)
        _transferLogsState.value = repository.getTransferLogs()
        _botLogsState.value = repository.getBotLogs()
    }

    fun clearLogs() {
        repository.clearAllLogs()
        _botLogsState.value = emptyList()
    }

    fun clearClosedPositions() {
        repository.clearClosedPositions()
        _positionsState.value = repository.getPositions()
    }

    private fun startPriceSimulation() {
        priceJob?.cancel()
        priceJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1500)
                val currentPrice = _btcPriceState.value
                val pctChange = (Random.nextDouble() - 0.49) * 0.002 // slight upward bias
                val newPrice = currentPrice * (1 + pctChange)
                _btcPriceState.value = newPrice

                // Update history
                val history = _priceHistoryState.value.toMutableList()
                history.add(newPrice)
                if (history.size > 20) {
                    history.removeAt(0)
                }
                _priceHistoryState.value = history

                // Update active positions PnL
                val activePos = repository.getPositions()
                var mutated = false
                activePos.forEach { pos ->
                    if (pos.status == "ACTIVE") {
                        val pnlMultiplier = if (pos.type == "LONG") 1 else -1
                        val rawDiff = (newPrice - pos.entryPrice) / pos.entryPrice
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
                    val emaShort = history.takeLast(3).average()
                    val emaLong = history.takeLast(5).average()

                    val activePositions = repository.getPositions().filter { it.status == "ACTIVE" }

                    if (emaShort > emaLong && activePositions.none { it.type == "LONG" }) {
                        // Bullish signal -> Open Long
                        withContext(Dispatchers.Main) {
                            executeManualOrder("LONG", Random.nextDouble(0.01, 0.05))
                        }
                    } else if (emaShort < emaLong && activePositions.none { it.type == "SHORT" }) {
                        // Bearish signal -> Open Short
                        withContext(Dispatchers.Main) {
                            executeManualOrder("SHORT", Random.nextDouble(0.01, 0.05))
                        }
                    }

                    // Auto-take-profit or stop-loss check
                    activePositions.forEach { pos ->
                        if (pos.pnlPercent >= 15.0) {
                            repository.addBotLog("INFO", "🎯 [Auto-Take-Profit] تم الوصول للربح المستهدف +15%. إغلاق تلقائي.")
                            withContext(Dispatchers.Main) {
                                closeActivePosition(pos.id)
                            }
                        } else if (pos.pnlPercent <= -10.0) {
                            repository.addBotLog("INFO", "🛡️ [Auto-Stop-Loss] تم كسر حد وقف الخسارة -10%. إغلاق احترازي.")
                            withContext(Dispatchers.Main) {
                                closeActivePosition(pos.id)
                            }
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
    }
}

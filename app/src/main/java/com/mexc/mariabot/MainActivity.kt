package com.mexc.mariabot

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.random.Random

// Color palette
val DarkBg = Color(0xFF090B0E)
val CardBg = Color(0xFF0F131A)
val CardInnerBg = Color(0xFF141B24)
val EmeraldNeon = Color(0xFF10B981)
val RoseDark = Color(0xFFF43F5E)
val AmberNeon = Color(0xFFF59E0B)
val BorderColor = Color(0xFF1C1917)
val TextGray = Color(0xFF94A3B8)

// Data Models
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
    val status: String = "ACTIVE" // "ACTIVE" or "CLOSED"
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

enum class DashboardTab {
    TRADING, REWARDS, BUILD, SETTINGS
}

class MariaBotViewModel : ViewModel() {
    private val _mexcConfig = MutableStateFlow(MEXCConfig())
    val mexcConfig: StateFlow<MEXCConfig> = _mexcConfig.asStateFlow()

    private val _spotBalance = MutableStateFlow(450.75)
    val spotBalance: StateFlow<Double> = _spotBalance.asStateFlow()

    private val _futuresBalance = MutableStateFlow(1250.0)
    val futuresBalance: StateFlow<Double> = _futuresBalance.asStateFlow()

    private val _btcPrice = MutableStateFlow(64250.0)
    val btcPrice: StateFlow<Double> = _btcPrice.asStateFlow()

    private val _priceHistory = MutableStateFlow(listOf(64210.0, 64230.0, 64220.0, 64245.0, 64235.0, 64250.0))
    val priceHistory: StateFlow<List<Double>> = _priceHistory.asStateFlow()

    private val _activePositions = MutableStateFlow<List<TradePosition>>(emptyList())
    val activePositions: StateFlow<List<TradePosition>> = _activePositions.asStateFlow()

    private val _closedPositions = MutableStateFlow<List<TradePosition>>(emptyList())
    val closedPositions: StateFlow<List<TradePosition>> = _closedPositions.asStateFlow()

    private val _rewardLogs = MutableStateFlow<List<RewardTransferLog>>(listOf(
        RewardTransferLog(
            id = "tx_1001",
            amount = 12.50,
            timestamp = System.currentTimeMillis() - 4 * 3600 * 1000
        ),
        RewardTransferLog(
            id = "tx_1002",
            amount = 5.25,
            timestamp = System.currentTimeMillis() - 24 * 3600 * 1000
        )
    ))
    val rewardLogs: StateFlow<List<RewardTransferLog>> = _rewardLogs.asStateFlow()

    private val _botLogs = MutableStateFlow<List<BotLog>>(listOf(
        BotLog(
            id = "log_2",
            timestamp = System.currentTimeMillis() - 90 * 1000,
            type = "SUCCESS",
            message = "تم إنشاء اتصال آمن بالشبكة المحلية لجهاز أندرويد LT_9904 بنجاح."
        ),
        BotLog(
            id = "log_1",
            timestamp = System.currentTimeMillis() - 120 * 1000,
            type = "INFO",
            message = "تطبيق مارية (Maria-Bot) قيد العمل والتأهب لحدث BTCUSDT."
        )
    ))
    val botLogs: StateFlow<List<BotLog>> = _botLogs.asStateFlow()

    init {
        startSimulationLoops()
    }

    private fun startSimulationLoops() {
        // Price fluctuation loop (every 1.5 seconds)
        viewModelScope.launch {
            while (isActive) {
                delay(1500)
                val current = _btcPrice.value
                val change = (Random.nextDouble() - 0.5) * 45
                val newPrice = (current + change).roundToTwoDecimals()
                _btcPrice.value = newPrice

                // Update price history
                val history = _priceHistory.value.toMutableList()
                history.add(newPrice)
                if (history.size > 20) {
                    history.removeAt(0)
                }
                _priceHistory.value = history

                // Update active positions P&L
                val active = _activePositions.value
                if (active.isNotEmpty()) {
                    _activePositions.value = active.map { pos ->
                        val priceDiff = newPrice - pos.entryPrice
                        val directionMult = if (pos.type == "LONG") 1.0 else -1.0
                        val rawReturn = (priceDiff / pos.entryPrice) * directionMult * pos.leverage
                        val pnl = (pos.amount * rawReturn).roundToTwoDecimals()
                        val pnlPercent = (rawReturn * 100).roundToTwoDecimals()
                        pos.copy(
                            currentPrice = newPrice,
                            pnl = pnl,
                            pnlPercent = pnlPercent
                        )
                    }
                }
            }
        }

        // Active positions expire checks loop (every 5 seconds)
        viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                val durationMs = _mexcConfig.value.eventDurationMinutes * 60 * 1000L
                val active = _activePositions.value.toMutableList()
                val toClose = mutableListOf<TradePosition>()
                val remaining = mutableListOf<TradePosition>()

                for (pos in active) {
                    if (now - pos.timestamp >= durationMs) {
                        toClose.add(pos)
                    } else {
                        remaining.add(pos)
                    }
                }

                if (toClose.isNotEmpty()) {
                    _activePositions.value = remaining
                    for (pos in toClose) {
                        val cost = pos.amount / pos.leverage
                        val finalPrice = _btcPrice.value
                        val priceDiff = finalPrice - pos.entryPrice
                        val directionMult = if (pos.type == "LONG") 1.0 else -1.0
                        val rawReturn = (priceDiff / pos.entryPrice) * directionMult * pos.leverage
                        val finalPnl = (pos.amount * rawReturn).roundToTwoDecimals()
                        val returnedAmount = (cost + finalPnl).roundToTwoDecimals()

                        _futuresBalance.value = (_futuresBalance.value + max(0.0, returnedAmount)).roundToTwoDecimals()

                        val closedPos = pos.copy(
                            currentPrice = finalPrice,
                            pnl = finalPnl,
                            pnlPercent = (rawReturn * 100).roundToTwoDecimals(),
                            status = "CLOSED"
                        )
                        _closedPositions.value = listOf(closedPos) + _closedPositions.value

                        addLog(
                            "INFO",
                            "انتهى حدث الـ ${_mexcConfig.value.eventDurationMinutes} دقائق: تم إغلاق صفقة ${pos.type} تلقائياً. سعر الإغلاق: $${finalPrice}. الأرباح والخسائر: $${finalPnl} USDT."
                        )
                    }
                }
            }
        }

        // Auto reward harvest simulation loop (every 35 seconds)
        viewModelScope.launch {
            while (isActive) {
                delay(35000)
                val config = _mexcConfig.value
                val spot = _spotBalance.value
                if (config.autoTransferRewards && spot > 0.0) {
                    _spotBalance.value = 0.0
                    _futuresBalance.value = (_futuresBalance.value + spot).roundToTwoDecimals()

                    val newLog = RewardTransferLog(
                        id = "tx_${System.currentTimeMillis()}",
                        amount = spot,
                        timestamp = System.currentTimeMillis()
                    )
                    _rewardLogs.value = listOf(newLog) + _rewardLogs.value
                    addLog("SUCCESS", "تلقائي: تم رصد وتحويل مكافأة بقيمة $${spot} USDT إلى محفظة العقود الآجلة بنجاح.")
                } else if (Random.nextDouble() > 0.8) {
                    val randomReward = (Random.nextDouble() * 15.0 + 2.0).roundToTwoDecimals()
                    _spotBalance.value = (_spotBalance.value + randomReward).roundToTwoDecimals()
                    addLog("INFO", "تم رصد مكافأة ترويجية جديدة بقيمة $${randomReward} USDT في محفظة الفوري (Spot).")
                }
            }
        }
    }

    fun addLog(type: String, message: String) {
        val newLog = BotLog(
            id = "log_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            type = type,
            message = message
        )
        _botLogs.value = listOf(newLog) + _botLogs.value
    }

    fun updateConfig(config: MEXCConfig) {
        _mexcConfig.value = config
        addLog("INFO", "تم تحديث إعدادات الاتصال وحجم الرافعة المالية لـ Maria Bot (رافعة: ${config.leverage}x).")
    }

    fun executeOrder(type: String, amountVal: Double): Boolean {
        val config = _mexcConfig.value
        val cost = amountVal / config.leverage
        if (_futuresBalance.value < cost) {
            addLog("ERROR", "فشل فتح صفقة $type: الرصيد المتاح غير كافٍ. التكلفة المطلوبة بالرافعة: $${cost.roundToTwoDecimals()} USDT.")
            return false
        }

        _futuresBalance.value = (_futuresBalance.value - cost).roundToTwoDecimals()

        val newPosition = TradePosition(
            id = "pos_${System.currentTimeMillis()}",
            type = type,
            entryPrice = _btcPrice.value,
            currentPrice = _btcPrice.value,
            amount = amountVal,
            leverage = config.leverage,
            timestamp = System.currentTimeMillis()
        )

        _activePositions.value = listOf(newPosition) + _activePositions.value
        addLog(
            "SUCCESS",
            "تم فتح صفقة عقود آجلة ثنائية لحدث ${config.eventDurationMinutes} دقائق: [${if (type == "LONG") "أعلى ↗" else "أدنى ↘"}] لزوج BTCUSDT بسعر دخول $${_btcPrice.value} بالرافعة المالية ${config.leverage}x."
        )
        return true
    }

    fun closePosition(id: String) {
        val active = _activePositions.value
        val pos = active.find { it.id == id } ?: return

        _activePositions.value = active.filter { it.id != id }

        val cost = pos.amount / pos.leverage
        val finalPrice = _btcPrice.value
        val priceDiff = finalPrice - pos.entryPrice
        val directionMult = if (pos.type == "LONG") 1.0 else -1.0
        val rawReturn = (priceDiff / pos.entryPrice) * directionMult * pos.leverage
        val finalPnl = (pos.amount * rawReturn).roundToTwoDecimals()
        val returnedAmount = (cost + finalPnl).roundToTwoDecimals()

        _futuresBalance.value = (_futuresBalance.value + max(0.0, returnedAmount)).roundToTwoDecimals()

        val closedPos = pos.copy(
            currentPrice = finalPrice,
            pnl = finalPnl,
            pnlPercent = (rawReturn * 100).roundToTwoDecimals(),
            status = "CLOSED"
        )

        _closedPositions.value = listOf(closedPos) + _closedPositions.value
        addLog("SUCCESS", "تم إغلاق صفقة ${pos.type} يدوياً فوراً. الربح/الخسارة المحقق: $${finalPnl} USDT.")
    }

    fun triggerManualTransfer() {
        val spot = _spotBalance.value
        if (spot <= 0.0) return

        _spotBalance.value = 0.0
        _futuresBalance.value = (_futuresBalance.value + spot).roundToTwoDecimals()

        val newLog = RewardTransferLog(
            id = "tx_${System.currentTimeMillis()}",
            amount = spot,
            timestamp = System.currentTimeMillis()
        )
        _rewardLogs.value = listOf(newLog) + _rewardLogs.value
        addLog("SUCCESS", "يدوي: تم تحويل مكافأة نقدية بقيمة $${spot} USDT إلى محفظة العقود الآجلة بنجاح.")
    }

    fun resetBalances() {
        _spotBalance.value = 150.0
        _futuresBalance.value = 1000.0
        _activePositions.value = emptyList()
        _closedPositions.value = emptyList()
        _rewardLogs.value = emptyList()
        addLog("WARNING", "تمت إعادة تعيين أرصدة محفظة التداول الافتراضية والصفقات النشطة.")
    }

    private fun Double.roundToTwoDecimals(): Double {
        return Math.round(this * 100.0) / 100.0
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = DarkBg,
                    surface = CardBg,
                    primary = EmeraldNeon,
                    onPrimary = Color.White,
                    secondary = AmberNeon,
                    error = RoseDark
                )
            ) {
                // Ensure RTL layout as specified by dir="rtl" in original
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = DarkBg
                    ) {
                        MariaBotDashboardScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun MariaBotDashboardScreen(viewModel: MariaBotViewModel = viewModel()) {
    val config by viewModel.mexcConfig.collectAsState()
    val btcPrice by viewModel.btcPrice.collectAsState()
    val priceHistory by viewModel.priceHistory.collectAsState()
    val spotBalance by viewModel.spotBalance.collectAsState()
    val futuresBalance by viewModel.futuresBalance.collectAsState()
    val activePositions by viewModel.activePositions.collectAsState()
    val closedPositions by viewModel.closedPositions.collectAsState()
    val rewardLogs by viewModel.rewardLogs.collectAsState()
    val botLogs by viewModel.botLogs.collectAsState()

    var activeTab by remember { mutableStateOf(DashboardTab.TRADING) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Decorative LED line at the very top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF047857),
                            Color(0xFF34D399),
                            Color(0xFF022C22)
                        )
                    )
                )
        )

        // Main Scrollable Area
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 800.dp

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dashboard Header
                item {
                    DashboardHeader(btcPrice, config)
                }

                // Navigation Tabs
                item {
                    NavigationTabs(selectedTab = activeTab, onTabSelected = { activeTab = it })
                }

                // Layout with Side-by-side on wide screens, stacked on small screens
                item {
                    if (isWideScreen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Main Side (8/12)
                            Box(modifier = Modifier.weight(0.65f)) {
                                TabContentSection(
                                    activeTab = activeTab,
                                    viewModel = viewModel,
                                    config = config,
                                    btcPrice = btcPrice,
                                    priceHistory = priceHistory,
                                    spotBalance = spotBalance,
                                    futuresBalance = futuresBalance,
                                    activePositions = activePositions,
                                    closedPositions = closedPositions,
                                    rewardLogs = rewardLogs
                                )
                            }

                            // Right Terminal Logs Side (4/12)
                            Column(
                                modifier = Modifier.weight(0.35f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SystemStatusDeck(config)
                                LiveConsoleLogs(botLogs)
                            }
                        }
                    } else {
                        // Vertical Stack
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TabContentSection(
                                activeTab = activeTab,
                                viewModel = viewModel,
                                config = config,
                                btcPrice = btcPrice,
                                priceHistory = priceHistory,
                                spotBalance = spotBalance,
                                futuresBalance = futuresBalance,
                                activePositions = activePositions,
                                closedPositions = closedPositions,
                                rewardLogs = rewardLogs
                            )

                            SystemStatusDeck(config)
                            LiveConsoleLogs(botLogs)
                        }
                    }
                }

                // Footer
                item {
                    FooterSection()
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(btcPrice: Double, config: MEXCConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Name & Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF064E3B), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Logo",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "تطبيق مارية (Maria-Bot)",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            Box(
                                modifier = Modifier
                                    .background(EmeraldNeon.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.25f), RoundedCornerShape(50.dp))
                                    .padding(horizontal = 8.dp, py = 2.dp)
                            ) {
                                Text(
                                    text = "إصدار الأجهزة LT_9904",
                                    color = EmeraldNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "لوحة التحكم الذكية لتداول عقود أحداث MEXC وتصميم سير البناء التلقائي",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Phone Environment Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardInnerBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF34D399), RoundedCornerShape(50.dp))
                        )
                        Text(
                            text = "بيئة الهاتف",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Device",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "LT_9904 (أندرويد 15)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // BTC Price
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardInnerBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "مؤشر حدث BTCUSDT",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", btcPrice)} USDT",
                        color = EmeraldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Connection Mode
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(CardInnerBg, RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "حالة الاتصال بـ MEXC",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (config.isSandbox) Icons.Default.Warning else Icons.Default.VerifiedUser,
                            contentDescription = "Status",
                            tint = if (config.isSandbox) AmberNeon else EmeraldNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (config.isSandbox) "محاكاة تجريبية" else "اتصال حي حقيقي",
                            color = if (config.isSandbox) AmberNeon else EmeraldNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationTabs(selectedTab: DashboardTab, onTabSelected: (DashboardTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            DashboardTab.TRADING to Pair("لوحة التداول والتحليل", Icons.Default.TrendingUp),
            DashboardTab.REWARDS to Pair("توريد المكافآت التلقائي", Icons.Default.Wallet),
            DashboardTab.BUILD to Pair("مولد GitHub لـ أندرويد", Icons.Default.Layers),
            DashboardTab.SETTINGS to Pair("إعدادات السير والأمان", Icons.Default.Settings)
        )

        tabs.forEach { (tab, info) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) EmeraldNeon else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = info.second,
                        contentDescription = info.first,
                        tint = if (isSelected) Color.White else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = info.first,
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TabContentSection(
    activeTab: DashboardTab,
    viewModel: MariaBotViewModel,
    config: MEXCConfig,
    btcPrice: Double,
    priceHistory: List<Double>,
    spotBalance: Double,
    futuresBalance: Double,
    activePositions: List<TradePosition>,
    closedPositions: List<TradePosition>,
    rewardLogs: List<RewardTransferLog>
) {
    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
        },
        label = "TabTransition"
    ) { targetTab ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (targetTab) {
                DashboardTab.TRADING -> {
                    PriceChart(priceHistory = priceHistory, currentPrice = btcPrice)
                    TradingDecisionCard(viewModel, futuresBalance, config)
                    ActivePositionsCard(activePositions, btcPrice, viewModel)
                }
                DashboardTab.REWARDS -> {
                    RewardsDashboardCard(viewModel, spotBalance, futuresBalance, config)
                    RewardLogsCard(rewardLogs)
                }
                DashboardTab.BUILD -> {
                    GitHubWorkflowBuilderCard(config)
                }
                DashboardTab.SETTINGS -> {
                    SettingsCard(viewModel, config)
                }
            }
        }
    }
}

@Composable
fun PriceChart(priceHistory: List<Double>, currentPrice: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Price Chart",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تخطيط حدث تذبذب BTC/USDT",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "تحديث فوري لكل ثانية ونصف يمثل أحداث الـ 10 دقائق الحية",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(EmeraldNeon.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .border(1.dp, EmeraldNeon.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, py = 4.dp)
                ) {
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", currentPrice)} USDT",
                        color = EmeraldNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (priceHistory.size > 1) {
                        val minVal = priceHistory.minOrNull() ?: 0.0
                        val maxVal = priceHistory.maxOrNull() ?: 0.0
                        val valRange = maxVal - minVal
                        val paddingRatio = 0.1f
                        val adjustedMin = minVal - (valRange * paddingRatio)
                        val adjustedMax = maxVal + (valRange * paddingRatio)
                        val range = if (adjustedMax - adjustedMin == 0.0) 1.0 else (adjustedMax - adjustedMin)

                        val width = size.width
                        val height = size.height

                        val points = priceHistory.mapIndexed { i, valPoint ->
                            val x = (i.toFloat() / (priceHistory.size - 1)) * width
                            val y = height - (((valPoint - adjustedMin) / range).toFloat() * (height * 0.8f) + (height * 0.1f))
                            Offset(x, y)
                        }

                        // Draw path gradient fill
                        val fillPath = Path().apply {
                            moveTo(points.first().x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldNeon.copy(alpha = 0.2f), Color.Transparent),
                                startY = 0f,
                                endY = height
                            )
                        )

                        // Draw path line
                        val linePath = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                lineTo(points[i].x, points[i].y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = EmeraldNeon,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }

                // Min / Max indicators
                if (priceHistory.isNotEmpty()) {
                    val minPrice = priceHistory.minOrNull() ?: 0.0
                    val maxPrice = priceHistory.maxOrNull() ?: 0.0
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .background(Color(0xFF0F131A).copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, py = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "أدنى: $${String.format(Locale.US, "%,.2f", minPrice)}",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "|",
                            color = Color.DarkGray,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "أعلى: $${String.format(Locale.US, "%,.2f", maxPrice)}",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingDecisionCard(viewModel: MariaBotViewModel, futuresBalance: Double, config: MEXCConfig) {
    var orderAmountText by remember { mutableStateOf("50") }
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "اتخاذ قرار حدث الـ ${config.eventDurationMinutes} دقائق لـ BTCUSDT",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "سيقوم الروبوت Maria-Bot بفتح صفقة سريعة وإغلاقها تلقائياً بعد مرور ${config.eventDurationMinutes} دقائق بناءً على توقعك.",
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sizing input
                Column(modifier = Modifier.weight(1.5f)) {
                    Text(
                        text = "حجم الصفقة الإجمالي ($ USDT)",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = orderAmountText,
                        onValueChange = { orderAmountText = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF090B0E),
                            unfocusedContainerColor = Color(0xFF090B0E),
                            focusedBorderColor = EmeraldNeon,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            Text(
                                text = "USDT",
                                color = TextGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        },
                        singleLine = true
                    )
                }

                // Leverage status
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الرافعة الفعالة المحددة",
                        color = TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${config.leverage}x",
                                color = EmeraldNeon,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "من الإعدادات",
                                color = Color.DarkGray,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val amount = orderAmountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            Toast.makeText(context, "الرجاء إدخال حجم صفقة صحيح", Toast.LENGTH_SHORT).show()
                        } else {
                            val success = viewModel.executeOrder("LONG", amount)
                            if (!success) {
                                val cost = amount / config.leverage
                                Toast.makeText(context, "فشل فتح الصفقة. الرصيد غير كافٍ. التكلفة: $cost USDT", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "أعلى (توقع صعود السعر) ↗",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "شراء عقود آجل صعودية (Long)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        val amount = orderAmountText.toDoubleOrNull()
                        if (amount == null || amount <= 0.0) {
                            Toast.makeText(context, "الرجاء إدخال حجم صفقة صحيح", Toast.LENGTH_SHORT).show()
                        } else {
                            val success = viewModel.executeOrder("SHORT", amount)
                            if (!success) {
                                val cost = amount / config.leverage
                                Toast.makeText(context, "فشل فتح الصفقة. الرصيد غير كافٍ. التكلفة: $cost USDT", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "أدنى (توقع هبوط السعر) ↘",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "شراء عقود آجل هبوطية (Short)",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ActivePositionsCard(activePositions: List<TradePosition>, btcPrice: Double, viewModel: MariaBotViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Active positions",
                    tint = EmeraldNeon,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "الصفقات النشطة قيد المراقبة",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (activePositions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد صفقات نشطة حالياً. حدد توقعك بالأعلى لتشغيل صفقات الحدث.",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activePositions.forEach { pos ->
                        val isProfit = pos.pnl >= 0.0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (pos.type == "LONG") EmeraldNeon.copy(alpha = 0.1f) else RoseDark.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, py = 4.dp)
                                ) {
                                    Text(
                                        text = if (pos.type == "LONG") "أعلى ↗" else "أدنى ↘",
                                        color = if (pos.type == "LONG") EmeraldNeon else RoseDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column {
                                    Text(
                                        text = "${pos.pair} (${pos.leverage}x)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الدخول: $${pos.entryPrice} | الحالي: $${btcPrice}",
                                        color = Color.DarkGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "العائد والربح المحقق",
                                    color = Color.DarkGray,
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${if (isProfit) "+" else ""}${pos.pnl} USDT (${pos.pnlPercent}%)",
                                    color = if (isProfit) EmeraldNeon else RoseDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = { viewModel.closePosition(pos.id) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1C0D11),
                                    contentColor = Color(0xFFFCA5A5)
                                ),
                                border = BorderStroke(1.dp, RoseDark.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, py = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "إغلاق فوراً",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsDashboardCard(viewModel: MariaBotViewModel, spotBalance: Double, futuresBalance: Double, config: MEXCConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "نظام سحب وتوريد المكافآت التلقائي",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تتراكم مكافآت الترويج في محفظة الفوري (Spot). يقوم تطبيق \"مارية\" بتحويلها تلقائياً إلى محفظة الآجل لدعم ميزانية الصفقات الجديدة.",
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Spot Wallet Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090B0E)),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(AmberNeon, RoundedCornerShape(50.dp))
                            )
                            Text("رصيد المكافآت الجديد", color = AmberNeon, fontSize = 7.sp)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("محفظة الفوري (Spot Wallet)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", spotBalance)} USDT",
                                color = AmberNeon,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("رصيد بانتظار التوريد للآجل", color = Color.DarkGray, fontSize = 8.sp)
                        }
                    }
                }

                // Futures Target Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090B0E)),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(EmeraldNeon, RoundedCornerShape(50.dp))
                            )
                            Text("نشط للآجل", color = EmeraldNeon, fontSize = 7.sp)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("محفظة الآجل (Futures Wallet)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", futuresBalance)} USDT",
                                color = EmeraldNeon,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("رصيد التداول الفعلي المتاح", color = Color.DarkGray, fontSize = 8.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Auto Toggle & Manual Transfer Control block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardInnerBg, RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = config.autoTransferRewards,
                        onCheckedChange = {
                            viewModel.updateConfig(config.copy(autoTransferRewards = it))
                        },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldNeon)
                    )
                    Text(
                        text = "تفعيل التوريد التلقائي الفوري للمكافآت المتراكمة",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.triggerManualTransfer() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                        Text(text = "تحويل وتوريد المكافآت يدوياً الآن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RewardLogsCard(rewardLogs: List<RewardTransferLog>) {
    val sdf = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar", "EG")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "History",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "سجل عمليات نقل وتحويل المكافآت",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rewardLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(12.dp))
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد عمليات تحويل مسجلة بعد.",
                        color = Color.DarkGray,
                        fontSize = 11.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rewardLogs.forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(EmeraldNeon.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .border(1.dp, EmeraldNeon.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, py = 3.dp)
                                ) {
                                    Text(
                                        text = "TRANSFER",
                                        color = EmeraldNeon,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column {
                                    Text(
                                        text = "تحويل رصيد بقيمة $${log.amount} ${log.asset}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${log.fromAccount} ← ${log.toAccount}",
                                        color = Color.DarkGray,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("مكتملة بنجاح", color = EmeraldNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    color = Color.DarkGray,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubWorkflowBuilderCard(config: MEXCConfig) {
    var gitBranch by remember { mutableStateOf("main") }
    var appName by remember { mutableStateOf("MariaBot") }
    var packageName by remember { mutableStateOf("com.mexc.mariabot") }
    var keystoreAlias by remember { mutableStateOf("upload") }
    var keystorePass by remember { mutableStateOf("malek-321") }

    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val actionsYaml = remember(gitBranch, appName, packageName, keystorePass, keystoreAlias) {
        """name: Maria Bot Fast Build & Sign

on:
  push:
    branches: [ "$gitBranch" ]

jobs:
  build:
    name: Build & Sign Release Bundle
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Source Code
      uses: actions/checkout@v4

    - name: Set up Java JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle # تفعيل الكاش لتسريع البناء الفائق لأقل من دقيقة

    - name: Decode and Prepare Keystore
      run: |
        echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 --decode > app/upload-keystore.jks

    - name: Build Release APK & AAB Bundle
      run: ./gradlew assembleRelease bundleRelease
      env:
        CM_KEYSTORE_PASSWORD: ${{ secrets.STORE_PASSWORD }}
        APP_NAME: "$appName"
        PACKAGE_NAME: "$packageName"

    - name: Upload Finished APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: $appName-Release-APK
        path: app/build/outputs/apk/release/*.apk

    - name: Upload Finished AAB Artifact
      uses: actions/upload-artifact@v4
      with:
        name: $appName-Release-AAB
        path: app/build/outputs/bundle/release/*.aab"""
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Cpu,
                        contentDescription = "Builder",
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "مُنظّم ومولّد ملف GitHub Actions لبناء أندرويد 15",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "صمم مخصصات بناء تطبيق Maria-Bot لتتمكن من تجميع كود الاندرويد عبر خوادم جيت هاب تلقائياً وتثبيته على جهازك LT_9904.",
                    color = TextGray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input form grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("اسم التطبيق (AppName)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = appName,
                                onValueChange = { appName = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090B0E),
                                    unfocusedContainerColor = Color(0xFF090B0E),
                                    focusedBorderColor = EmeraldNeon,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("اسم الحزمة التعريفية (PackageName)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = packageName,
                                onValueChange = { packageName = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090B0E),
                                    unfocusedContainerColor = Color(0xFF090B0E),
                                    focusedBorderColor = EmeraldNeon,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("فرع كود المصدر (Branch)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = gitBranch,
                                onValueChange = { gitBranch = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090B0E),
                                    unfocusedContainerColor = Color(0xFF090B0E),
                                    focusedBorderColor = EmeraldNeon,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("اسم مستعار للمفتاح المعياري (Alias)", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                            OutlinedTextField(
                                value = keystoreAlias,
                                onValueChange = { keystoreAlias = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF090B0E),
                                    unfocusedContainerColor = Color(0xFF090B0E),
                                    focusedBorderColor = EmeraldNeon,
                                    unfocusedBorderColor = BorderColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                singleLine = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // YAML Code Display with copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الكود الناتج لـ .github/workflows/main.yml",
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("ActionsYaml", actionsYaml)
                            clipboardManager.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الكود بالكامل!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon.copy(alpha = 0.1f),
                            contentColor = EmeraldNeon
                        ),
                        border = BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, py = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(12.dp))
                            Text("نسخ الكود بالكامل", fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Code Display window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = actionsYaml,
                        color = Color(0xFFCBD5E1),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Guides Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = "Book",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "دليل التوقيع المخصص وتجهيز الموازنة لأجهزة LT_9904",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Step 1
                    GuideStepItem(
                        number = "1",
                        title = "توليد مفتاح Keystore على الهاتف عبر تطبيق Termux",
                        description = "افتح محاكي Termux على جهازك LT_9904 ونفذ الأمر التالي لتوليد مفتاح أمان مخصص لأندرويد 15:",
                        command = "keytool -genkey -v -keystore upload-keystore.jks -alias $keystoreAlias -keyalg RSA -keysize 2048 -validity 10000",
                        clipboardManager = clipboardManager,
                        context = context
                    )

                    // Step 2
                    GuideStepItem(
                        number = "2",
                        title = "تحويل ملف jks إلى صيغة Base64 لإضافته إلى أسرار GitHub",
                        description = "قم بتحويل الملف upload-keystore.jks إلى صيغة نصية لكي تتمكن من حفظه بأمان في مستودع GitHub Secrets:",
                        command = "base64 -w0 upload-keystore.jks > keystore_base64.txt",
                        clipboardManager = clipboardManager,
                        context = context
                    )

                    // Step 3
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardInnerBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(EmeraldNeon.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                                    .border(1.dp, EmeraldNeon.copy(alpha = 0.3f), RoundedCornerShape(50.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("3", color = EmeraldNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "تكوين الأسرار في GitHub Repository Secrets",
                                color = EmeraldNeon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "اذهب إلى مستودع الكود الخاص بك في GitHub > Settings > Secrets and Variables > Actions، وأضف الأسرار التالية:",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "• KEYSTORE_BASE64: محتوى ملف keystore_base64.txt الناتج من الخطوة السابقة.",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "• STORE_PASSWORD: كلمة المرور التي اخترتها للمفتاح (مثل: $keystorePass).",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GuideStepItem(
    number: String,
    title: String,
    description: String,
    command: String,
    clipboardManager: ClipboardManager,
    context: Context
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardInnerBg, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(EmeraldNeon.copy(alpha = 0.1f), RoundedCornerShape(50.dp))
                    .border(1.dp, EmeraldNeon.copy(alpha = 0.3f), RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = EmeraldNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = title,
                color = EmeraldNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = description,
            color = Color.LightGray,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Copyable command block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF090B0E), RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, py = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = command,
                color = Color.LightGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Left,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText("termux_cmd", command)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "تم النسخ!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsCard(viewModel: MariaBotViewModel, config: MEXCConfig) {
    var apiKey by remember { mutableStateOf("") }
    var apiSecret by remember { mutableStateOf("") }
    var expandedLeverage by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "إعدادات الاتصال بموقع تداول MEXC",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "يمكنك ربط مفاتيح واجهة برمجة التطبيقات (API Keys) للاتصال المباشر بحسابك على MEXC، أو مواصلة استخدام حساب المحاكاة الافتراضي الآمن.",
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // API key fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("مفتاح API Key", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF090B0E),
                                unfocusedContainerColor = Color(0xFF090B0E),
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            placeholder = { Text("e.g. mx0v9s8df...", color = Color.DarkGray, fontSize = 10.sp) },
                            singleLine = true
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("مفتاح السر API Secret", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        OutlinedTextField(
                            value = apiSecret,
                            onValueChange = { apiSecret = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF090B0E),
                                unfocusedContainerColor = Color(0xFF090B0E),
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("••••••••••••••••••••", color = Color.DarkGray, fontSize = 10.sp) },
                            singleLine = true
                        )
                    }
                }

                // Leverage & Sandbox row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sandbox toggle
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardInnerBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor)
                            .clickable { viewModel.updateConfig(config.copy(isSandbox = !config.isSandbox)) }
                            .padding(horizontal = 12.dp, py = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("وضع المحاكاة التجريبية", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("تمكين التداول الافتراضي الآمن", color = Color.DarkGray, fontSize = 8.sp)
                        }
                        Checkbox(
                            checked = config.isSandbox,
                            onCheckedChange = { viewModel.updateConfig(config.copy(isSandbox = it)) },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldNeon)
                        )
                    }

                    // Leverage multiplier selector dropdown
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(CardInnerBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor)
                            .clickable { expandedLeverage = true }
                            .padding(horizontal = 12.dp, py = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("الرافعة المالية", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("قيمة تضخيم الصفقات", color = Color.DarkGray, fontSize = 8.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("${config.leverage}x", color = EmeraldNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.Gray)
                            }
                        }

                        DropdownMenu(
                            expanded = expandedLeverage,
                            onDismissRequest = { expandedLeverage = false },
                            modifier = Modifier.background(CardInnerBg)
                        ) {
                            val leverages = listOf(10, 20, 50, 100)
                            leverages.forEach { lev ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lev}x " + when(lev) {
                                                10 -> "(منخفض المخاطر)"
                                                20 -> "(متوسط موازن)"
                                                50 -> "(مخاطر مرتفعة)"
                                                else -> "(قوة قصوى)"
                                            },
                                            color = if (config.leverage == lev) EmeraldNeon else Color.White,
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateConfig(config.copy(leverage = lev))
                                        expandedLeverage = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Settings buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (apiKey.isNotBlank()) {
                                viewModel.updateConfig(config.copy(apiKey = apiKey, apiSecret = apiSecret))
                                Toast.makeText(context, "تمت محاكاة ربط وتفعيل حساب MEXC الخاص بك بنجاح!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "يرجى إدخال مفتاح API Key أولاً.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("ربط وحفظ مفاتيح الاتصال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.resetBalances() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1917)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("إعادة تعيين رصيد الحساب", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun SystemStatusDeck(config: MEXCConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تحديثات النظام الحية",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(EmeraldNeon, RoundedCornerShape(50.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Transfer Status
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF090B0E), RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("توريد المكافآت التلقائي", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (config.autoTransferRewards) "مفعّل ونشط" else "متوقف",
                        color = if (config.autoTransferRewards) EmeraldNeon else RoseDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Futures timing
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFF090B0E), RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor)
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("وقت حدث الآجل", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${config.eventDurationMinutes} دقائق حية",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveConsoleLogs(botLogs: List<BotLog>) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Logs",
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "وحدة السجلات والعمليات الحية (Maria-Logs)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    text = "15:15 UTC",
                    color = Color.DarkGray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable terminal screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF090B0E), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(botLogs, key = { it.id }) { log ->
                        val logColor = when (log.type) {
                            "SUCCESS" -> EmeraldNeon
                            "WARNING" -> AmberNeon
                            "ERROR" -> RoseDark
                            else -> Color(0xFF60A5FA)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = Color.DarkGray.copy(alpha = 0.05f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sdf.format(Date(log.timestamp)),
                                    color = Color.DarkGray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "[${log.type}]",
                                    color = logColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = log.message,
                                color = Color(0xFFE2E8F0),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FooterSection() {
    Divider(color = Color(0xFF1C1917), modifier = Modifier.padding(top = 16.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "© 2026 تطبيق مارية لتداول أحداث MEXC وسير بناء Android 15.",
            color = Color.DarkGray,
            fontSize = 9.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "لوحة تحكم مشفرة ومؤمنة بالكامل",
                color = Color.DarkGray,
                fontSize = 9.sp
            )
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Secured",
                tint = EmeraldNeon,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

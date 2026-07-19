package com.mexc.mariabot.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mexc.mariabot.model.*
import com.mexc.mariabot.ui.MariaBotViewModel
import com.mexc.mariabot.ui.theme.*
import com.mexc.mariabot.ui.components.InteractiveCandlestickChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MariaBotViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val isAutoTradingActive by viewModel.isAutoTradingActive.collectAsState()
    val btcPrice by viewModel.btcPriceState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCanvas),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(if (isAutoTradingActive) EmeraldNeon else Color.Gray)
                        )
                        Text(
                            text = "MARIA BOT • MEXC AUTOMATION",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(CardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$${String.format("%.2f", btcPrice)} USDT",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkCanvas,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == DashboardTab.DASHBOARD,
                    onClick = { viewModel.changeTab(DashboardTab.DASHBOARD) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.MARKETS,
                    onClick = { viewModel.changeTab(DashboardTab.MARKETS) },
                    icon = { Icon(Icons.Default.ShowChart, contentDescription = "الأسواق") },
                    label = { Text("الأسواق", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.FUTURES,
                    onClick = { viewModel.changeTab(DashboardTab.FUTURES) },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "العقود") },
                    label = { Text("العقود", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.WALLET,
                    onClick = { viewModel.changeTab(DashboardTab.WALLET) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "المحفظة") },
                    label = { Text("المحفظة", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.ORDERS,
                    onClick = { viewModel.changeTab(DashboardTab.ORDERS) },
                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "العمليات") },
                    label = { Text("العمليات", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.SETTINGS,
                    onClick = { viewModel.changeTab(DashboardTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                    label = { Text("الإعدادات", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCanvas)
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                DashboardTab.DASHBOARD -> DashboardTabContent(viewModel)
                DashboardTab.MARKETS -> MarketsTabContent(viewModel)
                DashboardTab.FUTURES -> FuturesTabContent(viewModel)
                DashboardTab.WALLET -> WalletTabContent(viewModel)
                DashboardTab.ORDERS -> OrdersTabContent(viewModel)
                DashboardTab.SETTINGS -> SettingsTabContent(viewModel)
            }
        }
    }
}

// ==================== 1. DASHBOARD TAB ====================
@Composable
fun DashboardTabContent(viewModel: MariaBotViewModel) {
    val isAutoActive by viewModel.isAutoTradingActive.collectAsState()
    val logs by viewModel.botLogsState.collectAsState()
    val insight by viewModel.marketInsightState.collectAsState()
    val news by viewModel.newsState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("نظام التداول التلقائي الخوارزمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("التشغيل الذاتي المباشر على منصة MEXC", color = TextGray, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isAutoActive,
                            onCheckedChange = { viewModel.toggleAutoTrading() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldNeon,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = BorderColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("بيئة التشغيل المادية", color = TextGray, fontSize = 11.sp)
                            Text("جهاز LT_9904 المدمج", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("مزامنة التوقيت", color = TextGray, fontSize = 11.sp)
                            Text("متصل • زمن حقيقي", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تحليل الذكاء الاصطناعي والمؤشرات", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("مؤشر القوة النسبية RSI", color = TextGray, fontSize = 11.sp)
                            Text(
                                text = String.format("%.2f", insight?.rsi ?: 50.0),
                                color = if ((insight?.rsi ?: 50.0) > 70.0) TextRed else if ((insight?.rsi ?: 50.0) < 30.0) TextGreen else EmeraldNeon,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("التذبذب التاريخي", color = TextGray, fontSize = 11.sp)
                            Text(
                                text = insight?.volatility ?: "LOW",
                                color = EmeraldNeon,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الاتجاه المقترح", color = TextGray, fontSize = 11.sp)
                            val side = insight?.suggestedSignal ?: "HOLD_NEUTRAL"
                            Text(
                                text = if (side == "BUY_LONG") "شراء / LONG" else if (side == "SELL_SHORT") "بيع / SHORT" else "انتظار",
                                color = if (side == "BUY_LONG") TextGreen else if (side == "SELL_SHORT") TextRed else TextGray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        if (news.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("الأخبار الاقتصادية المباشرة (BTC)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        news.take(3).forEach { article ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(article.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(article.source, color = TextGray, fontSize = 10.sp)
                                    Text(
                                        if (article.sentiment == "BULLISH") "إيجابي" else if (article.sentiment == "BEARISH") "سلبي" else "محايد",
                                        color = if (article.sentiment == "BULLISH") TextGreen else if (article.sentiment == "BEARISH") TextRed else TextGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Divider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("عمليات التشغيل الفورية والذكاء الاصطناعي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(reverseLayout = false) {
                            items(logs.take(15)) { log ->
                                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                    val color = when (log.type) {
                                        "SUCCESS" -> TextGreen
                                        "WARNING" -> AmberAccent
                                        "ERROR" -> TextRed
                                        else -> TextGray
                                    }
                                    Text("[${log.type}] ", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Text(log.message, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 2. MARKETS TAB ====================
@Composable
fun MarketsTabContent(viewModel: MariaBotViewModel) {
    val candles by viewModel.candlesState.collectAsState()
    val currentPrice by viewModel.btcPriceState.collectAsState()
    val selectedInterval by viewModel.selectedIntervalState.collectAsState()

    val intervals = listOf("1m", "5m", "15m", "1h", "4h", "1d")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("BTCUSDT الفوري", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text("العقد والرسومات البيانية الحية لـ MEXC", color = TextGray, fontSize = 12.sp)
                        }
                        Text(
                            text = "$${String.format("%.2f", currentPrice)}",
                            color = EmeraldNeon,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("أعلى سعر 24ساعة", color = TextGray, fontSize = 11.sp)
                            val high = if (candles.isNotEmpty()) candles.maxOf { it.high } else currentPrice * 1.01
                            Text("$${String.format("%.1f", high)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column {
                            Text("أدنى سعر 24ساعة", color = TextGray, fontSize = 11.sp)
                            val low = if (candles.isNotEmpty()) candles.minOf { it.low } else currentPrice * 0.99
                            Text("$${String.format("%.1f", low)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("حجم التداول", color = TextGray, fontSize = 11.sp)
                            val vol = if (candles.isNotEmpty()) candles.sumOf { it.volume } else 1850.5
                            Text(String.format("%.2f BTC", vol), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                intervals.forEach { interval ->
                    val selected = selectedInterval == interval
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) EmeraldNeon else CardBg,
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, if (selected) EmeraldNeon else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { viewModel.loadKlines(interval) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = interval,
                            color = if (selected) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            InteractiveCandlestickChart(
                candles = candles,
                latestPrice = currentPrice
            )
        }
    }
}

// ==================== 3. FUTURES MANUAL TAB ====================
@Composable
fun FuturesTabContent(viewModel: MariaBotViewModel) {
    val positions by viewModel.positionsState.collectAsState()
    val btcPrice by viewModel.btcPriceState.collectAsState()

    var manualAmount by remember { mutableStateOf("0.02") }
    var manualStopLoss by remember { mutableStateOf("") }
    var manualTakeProfit by remember { mutableStateOf("") }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("منصة تداول العقود المباشرة (BTCUSDT)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text("الكمية بالعقود (BTC)", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = manualStopLoss,
                            onValueChange = { manualStopLoss = it },
                            label = { Text("وقف الخسارة (SL)", color = TextGray, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = manualTakeProfit,
                            onValueChange = { manualTakeProfit = it },
                            label = { Text("أخذ الأرباح (TP)", color = TextGray, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = manualAmount.toDoubleOrNull() ?: 0.0
                                val sl = manualStopLoss.toDoubleOrNull()
                                val tp = manualTakeProfit.toDoubleOrNull()
                                if (amt > 0) {
                                    viewModel.executeManualOrder("LONG", amt, sl, tp)
                                    Toast.makeText(context, "تم إرسال أمر LONG بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TextGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فتح LONG", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val amt = manualAmount.toDoubleOrNull() ?: 0.0
                                val sl = manualStopLoss.toDoubleOrNull()
                                val tp = manualTakeProfit.toDoubleOrNull()
                                if (amt > 0) {
                                    viewModel.executeManualOrder("SHORT", amt, sl, tp)
                                    Toast.makeText(context, "تم إرسال أمر SHORT بنجاح", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TextRed),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("فتح SHORT", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("المراكز المفتوحة النشطة (${positions.count { it.status == "ACTIVE" }})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        val activeList = positions.filter { it.status == "ACTIVE" }
        if (activeList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد مراكز نشطة مفتوحة حالياً.", color = TextGray, fontSize = 12.sp)
                }
            }
        } else {
            items(activeList) { pos ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(if (pos.type == "LONG") TextGreen else TextRed, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(pos.type, color = if (pos.type == "LONG") Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pos.pair, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${pos.leverage}x", color = EmeraldNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.closeActivePosition(pos.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = TextRed.copy(alpha = 0.2f), contentColor = TextRed),
                                border = BorderStroke(1.dp, TextRed),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("إغلاق المركز", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("سعر الدخول", color = TextGray, fontSize = 11.sp)
                                Text("$${String.format("%.1f", pos.entryPrice)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column {
                                Text("السعر الحالي", color = TextGray, fontSize = 11.sp)
                                Text("$${String.format("%.1f", pos.currentPrice)}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("أرباح/خسائر (USDT)", color = TextGray, fontSize = 11.sp)
                                val color = if (pos.pnl >= 0) TextGreen else TextRed
                                val sign = if (pos.pnl >= 0) "+" else ""
                                Text(
                                    text = "$sign${String.format("%.2f", pos.pnl)} (${sign}${String.format("%.2f", pos.pnlPercent)}%)",
                                    color = color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
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

// ==================== 4. WALLET TAB ====================
@Composable
fun WalletTabContent(viewModel: MariaBotViewModel) {
    var subTab by remember { mutableStateOf(0) }
    val spotList by viewModel.spotWalletState.collectAsState()
    val futuresData by viewModel.futuresWalletState.collectAsState()
    val btcPrice by viewModel.btcPriceState.collectAsState()
    val positions by viewModel.positionsState.collectAsState()

    val activePositions = positions.filter { it.status == "ACTIVE" }
    val totalUnrealizedPnl = activePositions.sumOf { it.pnl }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("محفظة فوري / Spot", "محفظة عقود / Futures").forEachIndexed { index, name ->
                val selected = subTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) EmeraldNeon else CardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, if (selected) EmeraldNeon else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { subTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name,
                        color = if (selected) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (subTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val totalEst = spotList.sumOf { asset ->
                        val balance = asset.free.toDoubleOrNull() ?: 0.0
                        if (asset.asset == "USDT") balance
                        else if (asset.asset == "BTC") balance * btcPrice
                        else if (asset.asset == "MX") balance * 5.0
                        else if (asset.asset == "ETH") balance * 3500.0
                        else 0.0
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("إجمالي القيمة المقدرة للمحفظة الفورية", color = TextGray, fontSize = 12.sp)
                            Text("$${String.format("%.2f", totalEst)} USDT", color = EmeraldNeon, fontWeight = FontWeight.Black, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                items(spotList) { asset ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.6f)),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = asset.asset,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                val valUsdt = (asset.free.toDoubleOrNull() ?: 0.0) * (if (asset.asset == "BTC") btcPrice else if (asset.asset == "MX") 5.0 else if (asset.asset == "ETH") 3500.0 else 1.0)
                                Text(
                                    text = "$${String.format("%.2f", valUsdt)} USDT",
                                    color = EmeraldNeon,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الرصيد المتاح (Available Balance):",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = asset.free,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "الرصيد المجمد (Frozen Balance):",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = asset.locked,
                                    color = TextRed.copy(alpha = 0.8f),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val data = futuresData
            val rawWalletBalance = (data?.availableBalance ?: 0.0) + (data?.positionMargin ?: 0.0)
            val walletBalanceWithPnl = rawWalletBalance + totalUnrealizedPnl
            val marginRatio = if (walletBalanceWithPnl > 0) {
                ((data?.positionMargin ?: 0.0) / walletBalanceWithPnl) * 100.0
            } else 0.0

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("رصيد محفظة العقود الإجمالي (Wallet Balance)", color = TextGray, fontSize = 12.sp)
                            Text("$${String.format("%.2f", walletBalanceWithPnl)} USDT", color = EmeraldNeon, fontWeight = FontWeight.Black, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Available Margin
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الهامش المتاح (Available Margin)", color = TextGray, fontSize = 12.sp)
                                Text("$${String.format("%.2f", data?.availableBalance ?: 0.0)} USDT", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Unrealized PNL
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("أرباح/خسائر غير محققة (Unrealized PnL)", color = TextGray, fontSize = 12.sp)
                                val pnlColor = if (totalUnrealizedPnl >= 0) TextGreen else TextRed
                                val pnlSign = if (totalUnrealizedPnl >= 0) "+" else ""
                                Text("$pnlSign$${String.format("%.2f", totalUnrealizedPnl)} USDT", color = pnlColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Margin Ratio
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("نسبة الهامش (Margin Ratio)", color = TextGray, fontSize = 12.sp)
                                Text("${String.format("%.2f", marginRatio)}%", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("درجة المخاطرة ونظام الهامش (Risk Level)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            LinearProgressIndicator(
                                progress = (marginRatio / 100.0).toFloat().coerceIn(0f, 1f),
                                color = if (marginRatio > 50.0) RedNeon else if (marginRatio > 25.0) AmberAccent else EmeraldNeon,
                                trackColor = BorderColor,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val riskStr = if (marginRatio > 50.0) "مرتفعة (High)" else if (marginRatio > 20.0) "متوسطة (Medium)" else "آمنة (Low Risk)"
                                val riskColor = if (marginRatio > 50.0) TextRed else if (marginRatio > 20.0) AmberAccent else TextGreen
                                Text("معدل الاستخدام: ${String.format("%.1f", marginRatio)}%", color = TextGray, fontSize = 11.sp)
                                Text("مستوى المخاطرة: $riskStr", color = riskColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.8f)),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("المراكز المفتوحة (Open Positions): ${activePositions.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (activePositions.isEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد مراكز نشطة مفتوحة حالياً.", color = TextGray, fontSize = 11.sp)
                            } else {
                                activePositions.forEach { pos ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${pos.type} ${pos.pair} ${pos.leverage}x", color = if (pos.type == "LONG") TextGreen else TextRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("$${String.format("%.2f", pos.pnl)} USDT (${String.format("%.2f", pos.pnlPercent)}%)", color = if (pos.pnl >= 0) TextGreen else TextRed, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== 5. ORDERS TAB ====================
@Composable
fun OrdersTabContent(viewModel: MariaBotViewModel) {
    val logs by viewModel.botLogsState.collectAsState()
    val transfers by viewModel.transferLogsState.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("سجل تحويل الأرباح والهامش", "سجل العمليات التقني").forEachIndexed { index, title ->
                val selected = activeSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (selected) EmeraldNeon else CardBg, RoundedCornerShape(8.dp))
                        .border(1.dp, if (selected) EmeraldNeon else BorderColor, RoundedCornerShape(8.dp))
                        .clickable { activeSubTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = title, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        if (activeSubTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (transfers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("لا توجد تحويلات آلية للأرباح بعد.", color = TextGray, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(transfers) { t ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("دعم هامش تلقائي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("+${t.amount} ${t.asset}", color = EmeraldNeon, fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("من ${t.fromAccount} إلى ${t.toAccount}", color = TextGray, fontSize = 11.sp)
                                    Text("مكتمل", color = TextGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("أحدث 100 خطوة تشغيلية", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.clearLogs() },
                        colors = ButtonDefaults.buttonColors(containerColor = TextRed.copy(alpha = 0.15f), contentColor = TextRed),
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("مسح السجل", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(logs) { log ->
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            val color = when (log.type) {
                                "SUCCESS" -> TextGreen
                                "WARNING" -> AmberAccent
                                "ERROR" -> TextRed
                                else -> TextGray
                            }
                            Text("[${log.type}] ", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(log.message, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==================== 6. SETTINGS TAB ====================
@Composable
fun SettingsTabContent(viewModel: MariaBotViewModel) {
    val config by viewModel.configState.collectAsState()
    val context = LocalContext.current

    var apiKey by remember(config) { mutableStateOf(config.apiKey) }
    var apiSecret by remember(config) { mutableStateOf(config.apiSecret) }
    var leverage by remember(config) { mutableStateOf(config.leverage.toString()) }
    var durationMin by remember(config) { mutableStateOf(config.eventDurationMinutes.toString()) }
    var sandbox by remember(config) { mutableStateOf(config.isSandbox) }

    var actionsSubTab by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("إعدادات الاتصال الآمن", "بناء CI/CD ومزامنة الغيم").forEachIndexed { index, name ->
                    val selected = actionsSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (selected) EmeraldNeon else CardBg, RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) EmeraldNeon else BorderColor, RoundedCornerShape(8.dp))
                            .clickable { actionsSubTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = name, color = if (selected) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        if (actionsSubTab == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("الربط والاتصال الآمن بواجهات MEXC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("كافة المفاتيح مشفرة ومحفوظة بـ Keystore للنظام", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            label = { Text("MEXC API Key (X-MEXC-APIKEY)", color = TextGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = apiSecret,
                            onValueChange = { apiSecret = it },
                            label = { Text("MEXC Secret Key", color = TextGray) },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وضع المحاكاة التجريبي (Sandbox)", color = Color.White, fontSize = 12.sp)
                            Switch(
                                checked = sandbox,
                                onCheckedChange = { sandbox = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldNeon
                                )
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("معاملات خوارزمية التداول السريع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = leverage,
                            onValueChange = { leverage = it },
                            label = { Text("الرافعة المالية للعقود (Leverage)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = durationMin,
                            onValueChange = { durationMin = it },
                            label = { Text("دورة الفحص والتحليل بالدقائق (Minutes)", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldNeon,
                                unfocusedBorderColor = BorderColor,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val levVal = leverage.toIntOrNull() ?: 20
                                val durVal = durationMin.toIntOrNull() ?: 10
                                val updated = MEXCConfig(
                                    apiKey = apiKey,
                                    apiSecret = apiSecret,
                                    leverage = levVal,
                                    eventDurationMinutes = durVal,
                                    isSandbox = sandbox
                                )
                                viewModel.updateConfig(updated)
                                Toast.makeText(context, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("حفظ وتأكيد الإعدادات والمفاتيح", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("مولد ومراقب حزم البناء CI/CD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("توليد ملفات GitHub Workflows وبناء حزم الإنتاج المباشر APK & AAB آلياً", color = TextGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("هيكل ملف سير العمل (.github/workflows/build.yml):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = """
name: Build Android Releases
on:
  push:
    branches: [ main ]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'zulu'
      - name: Build APK & AAB
        run: |
          chmod +x gradlew
          ./gradlew assembleRelease bundleRelease
                                        """.trimIndent(),
                                        color = EmeraldNeon,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val clipboard = LocalClipboardManager.current
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString("name: Build Android Releases..."))
                                Toast.makeText(context, "تم نسخ كود GitHub Actions بنجاح!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("نسخ إعدادات سير العمل CI/CD", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

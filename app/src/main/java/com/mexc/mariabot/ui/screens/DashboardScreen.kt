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
                            fontSize = 16.sp,
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
                            fontSize = 13.sp,
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
                    selected = activeTab == DashboardTab.TRADING,
                    onClick = { viewModel.changeTab(DashboardTab.TRADING) },
                    icon = { Icon(Icons.Default.TrendingUp, contentDescription = "تداول") },
                    label = { Text("تداول", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.REWARDS,
                    onClick = { viewModel.changeTab(DashboardTab.REWARDS) },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "مكافآت") },
                    label = { Text("المكافآت", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldNeon,
                        indicatorColor = EmeraldNeon,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    selected = activeTab == DashboardTab.BUILD,
                    onClick = { viewModel.changeTab(DashboardTab.BUILD) },
                    icon = { Icon(Icons.Default.Build, contentDescription = "بناء") },
                    label = { Text("بناء CI/CD", fontWeight = FontWeight.Bold) },
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = "إعدادات") },
                    label = { Text("الإعدادات", fontWeight = FontWeight.Bold) },
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
                DashboardTab.TRADING -> TradingTabContent(viewModel)
                DashboardTab.REWARDS -> RewardsTabContent(viewModel)
                DashboardTab.BUILD -> BuildTabContent()
                DashboardTab.SETTINGS -> SettingsTabContent(viewModel)
            }
        }
    }
}

// ==================== TRADING TAB ====================
@Composable
fun TradingTabContent(viewModel: MariaBotViewModel) {
    val isAutoTradingActive by viewModel.isAutoTradingActive.collectAsState()
    val positions by viewModel.positionsState.collectAsState()
    val btcPrice by viewModel.btcPriceState.collectAsState()
    val priceHistory by viewModel.priceHistoryState.collectAsState()
    val logs by viewModel.botLogsState.collectAsState()

    var manualAmount by remember { mutableStateOf("0.02") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        // Hero Section & Status Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Toggle AI Trading Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.toggleAutoTrading() },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAutoTradingActive) EmeraldNeon.copy(alpha = 0.08f) else CardBg
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isAutoTradingActive) EmeraldNeon else BorderColor
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isAutoTradingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isAutoTradingActive) EmeraldNeon else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isAutoTradingActive) "تداول تلقائي نشط" else "تفعيل التداول التلقائي",
                            color = if (isAutoTradingActive) EmeraldNeon else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Device / Target info
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "الأجهزة: LT_9904",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Price Chart Component
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "مخطط السعر الحقيقي لـ BTCUSDT",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PriceChart(priceHistory)
                }
            }
        }

        // Manual Trading Terminal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "محطة التداول اليدوية الآمنة",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text("كمية العقد (BTC)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon,
                            unfocusedBorderColor = BorderColor
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val amt = manualAmount.toDoubleOrNull() ?: 0.01
                                viewModel.executeManualOrder("LONG", amt)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TextGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فتح LONG (صعود)", color = Color.Black, fontWeight = FontWeight.Black)
                        }
                        Button(
                            onClick = {
                                val amt = manualAmount.toDoubleOrNull() ?: 0.01
                                viewModel.executeManualOrder("SHORT", amt)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TextRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("فتح SHORT (هبوط)", color = Color.White, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // Active Positions Card List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مراكز العقود الآجلة النشطة (${positions.count { it.status == "ACTIVE" }})",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                if (positions.any { it.status == "CLOSED" }) {
                    Text(
                        text = "مسح الصفقات المغلقة",
                        color = EmeraldNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { viewModel.clearClosedPositions() }
                    )
                }
            }
        }

        val activePositions = positions.filter { it.status == "ACTIVE" }
        if (activePositions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد صفقات مفتوحة حالياً.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(activePositions) { pos ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, if (pos.pnl >= 0) TextGreen.copy(alpha = 0.5f) else TextRed.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(if (pos.type == "LONG") TextGreen else TextRed, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(pos.type, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(pos.pair, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("x${pos.leverage}", color = Color.LightGray, fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { viewModel.closeActivePosition(pos.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق الصفقة", tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("سعر الدخول", color = Color.Gray, fontSize = 11.sp)
                                Text("$${String.format("%.2f", pos.entryPrice)}", color = Color.White, fontSize = 13.sp)
                            }
                            Column {
                                Text("السعر الحالي", color = Color.Gray, fontSize = 11.sp)
                                Text("$${String.format("%.2f", pos.currentPrice)}", color = Color.White, fontSize = 13.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("الأرباح/الخسائر (PnL)", color = Color.Gray, fontSize = 11.sp)
                                Text(
                                    text = "${if (pos.pnl >= 0) "+" else ""}${String.format("%.2f", pos.pnl)} USDT (${String.format("%.2f", pos.pnlPercent)}%)",
                                    color = if (pos.pnl >= 0) TextGreen else TextRed,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Console Log Card
        item {
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
                            text = "لوحة المراقبة والسجلات الحية",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(
                            onClick = { viewModel.clearLogs() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "مسح السجلات", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(logs) { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val logColor = when (log.type) {
                                        "SUCCESS" -> TextGreen
                                        "ERROR" -> TextRed
                                        "WARNING" -> AmberAccent
                                        else -> Color.Cyan
                                    }
                                    Text(
                                        text = "[${log.type}]",
                                        color = logColor,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = log.message,
                                        color = Color.White,
                                        fontSize = 11.sp,
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
}

// ==================== REWARDS TAB ====================
@Composable
fun RewardsTabContent(viewModel: MariaBotViewModel) {
    val config by viewModel.configState.collectAsState()
    val transferLogs by viewModel.transferLogsState.collectAsState()

    var manualRewardAmount by remember { mutableStateOf("50.0") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "نظام سحب المكافآت الترويجي الذكي",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "هذه التقنية تتولى رصد المكافآت المكتسبة في محفظة الفوري (Spot) وتحويلها دورياً وهامشياً كرافعة سيولة لتعزيز صفقات Futures المفتوحة بنجاح وآلياً.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Automation Control
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("التحويل التلقائي للمكافآت", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("نقل السيولة فورا لمحفظة العقود", color = Color.Gray, fontSize = 11.sp)
                    }
                    Switch(
                        checked = config.autoTransferRewards,
                        onCheckedChange = { viewModel.updateConfig(config.copy(autoTransferRewards = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = EmeraldNeon,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.Black
                        )
                    )
                }
            }
        }

        // Manual Harvest Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("سحب ونقل سيولة فوري (Spot ➔ Futures)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualRewardAmount,
                        onValueChange = { manualRewardAmount = it },
                        label = { Text("المبلغ (USDT)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            val amt = manualRewardAmount.toDoubleOrNull() ?: 10.0
                            viewModel.manualRewardTransfer(amt)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("بدء التحويل اليدوي الفوري", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Historic Logs list
        item {
            Text("تاريخ عمليات التحويل التلقائي واليدوي", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }

        if (transferLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد عمليات تحويل مسجلة.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        } else {
            items(transferLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${log.amount} ${log.asset}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("من: Spot • إلى: Futures", color = Color.Gray, fontSize = 11.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(TextGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(log.status, color = TextGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==================== BUILD TAB ====================
@Composable
fun BuildTabContent() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedBranch by remember { mutableStateOf("main") }

    val buildYaml = """
name: Maria Bot Build & Release

on:
  push:
    branches: [ "$selectedBranch" ]
  tags: [ "*" ]

permissions:
  contents: write

jobs:
  build:
    name: Build & Sign Android App
    runs-on: ubuntu-latest
    steps:
    - name: Checkout Source Code
      uses: actions/checkout@v4

    - name: Set up Java JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: 'gradle'

    - name: Prepare Keystore and key.properties
      run: |
        mkdir -p app
        echo "${'$'}{{ secrets.CM_KEYSTORE_BASE64 }}" | base64 --decode > app/release.keystore
        echo "storeFile=release.keystore" > key.properties
        echo "storePassword=${'$'}{{ secrets.CM_KEYSTORE_PASSWORD }}" >> key.properties
        echo "keyAlias=${'$'}{{ secrets.CM_KEY_ALIAS }}" >> key.properties
        echo "keyPassword=${'$'}{{ secrets.CM_KEYSTORE_PASSWORD }}" >> key.properties

    - name: Build Signed Release APK & AAB
      run: ./gradlew assembleRelease bundleRelease --no-daemon

    - name: Upload APK Release Artifact
      uses: actions/upload-artifact@v4
      with:
        name: MariaBot-Release-APK
        path: app/build/outputs/apk/release/*.apk

    - name: Create Tag Release
      uses: softprops/action-gh-release@v2
      if: startsWith(github.ref, 'refs/tags/')
      with:
        name: Maria Bot Release ${{'$'}}{{ github.ref_name }}
        files: |
          app/build/outputs/apk/release/*.apk
          app/build/outputs/bundle/release/*.aab
      env:
        GITHUB_TOKEN: ${'$'}{{ secrets.GITHUB_TOKEN }}
""".trimIndent()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "منشئ سير العمل التلقائي لـ GitHub Actions",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يقوم هذا المولد بتهيئة ملف YAML الأفضل لبناء وتوقيع تطبيق أندرويد حقيقي للأجهزة LT_9904 تلقائياً فور رفع الكود إلى GitHub.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تخصيص هدف البناء", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = selectedBranch,
                        onValueChange = { selectedBranch = it },
                        label = { Text("فرع البناء الرئيسي (Branch)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ملف الإعداد (main.yml)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(buildYaml))
                                Toast.makeText(context, "تم نسخ رمز YAML بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرمز", tint = EmeraldNeon)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = buildYaml,
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== SETTINGS TAB ====================
@Composable
fun SettingsTabContent(viewModel: MariaBotViewModel) {
    val config by viewModel.configState.collectAsState()

    var apiKeyInput by remember { mutableStateOf(config.apiKey) }
    var apiSecretInput by remember { mutableStateOf(config.apiSecret) }
    var isSandboxInput by remember { mutableStateOf(config.isSandbox) }
    var leverageInput by remember { mutableStateOf(config.leverage.toString()) }
    var durationInput by remember { mutableStateOf(config.eventDurationMinutes.toString()) }

    var passwordVisible by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تكامل واجهات MEXC API الرسمية", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // API Key
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("MEXC API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // API Secret
                    OutlinedTextField(
                        value = apiSecretInput,
                        onValueChange = { apiSecretInput = it },
                        label = { Text("MEXC API Secret") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = null, tint = Color.Gray)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )
                }
            }
        }

        // Mode Toggles & Config parameters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("معايير التداول والتحوط", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Sandbox Mode Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("وضع الحساب التجريبي (Sandbox)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("تداول آمن وهمي بدون مخاطرة بأموال حقيقية", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isSandboxInput,
                            onCheckedChange = { isSandboxInput = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldNeon
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Leverage
                    OutlinedTextField(
                        value = leverageInput,
                        onValueChange = { leverageInput = it },
                        label = { Text("الرافعة المالية للعقود الآجلة (Leverage)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Event check duration
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("مدة رصد الأحداث الدورية (دقائق)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldNeon,
                            focusedLabelColor = EmeraldNeon
                        )
                    )
                }
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = {
                    val lev = leverageInput.toIntOrNull() ?: 20
                    val dur = durationInput.toIntOrNull() ?: 10
                    viewModel.updateConfig(
                        config.copy(
                            apiKey = apiKeyInput,
                            apiSecret = apiSecretInput,
                            isSandbox = isSandboxInput,
                            leverage = lev,
                            eventDurationMinutes = dur
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("حفظ وتأكيد الإعدادات", color = Color.Black, fontWeight = FontWeight.Black)
            }
        }
    }
}

// Simple and highly optimized vector price chart component
@Composable
fun PriceChart(prices: List<Double>) {
    if (prices.size < 2) return

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 8.dp)
    ) {
        val maxPrice = prices.maxOrNull() ?: 1.0
        val minPrice = prices.minOrNull() ?: 0.0
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

        val width = size.width
        val height = size.height

        val path = Path()
        val stepX = width / (prices.size - 1)

        prices.forEachIndexed { index, price ->
            val normalizedY = ((price - minPrice) / priceRange)
            val x = index * stepX
            val y = height - (normalizedY * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = EmeraldNeon,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

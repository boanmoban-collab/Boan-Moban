package com.mexc.mariabot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mexc.mariabot.model.Candle
import com.mexc.mariabot.ui.theme.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun InteractiveCandlestickChart(
    candles: List<Candle>,
    latestPrice: Double
) {
    if (candles.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(CardBg, RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    var visibleCandlesCount by remember { mutableStateOf(40f) }
    var scrollOffset by remember { mutableStateOf(0f) }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    visibleCandlesCount = (visibleCandlesCount / zoom).coerceIn(15f, 120f)
                    val candleWidthPx = size.width / visibleCandlesCount
                    scrollOffset = (scrollOffset + pan.x / candleWidthPx).coerceIn(0f, maxOf(0f, candles.size - visibleCandlesCount))
                }
            }
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val rightMargin = 60.dp.toPx()
            val bottomMargin = 20.dp.toPx()

            val chartWidth = width - rightMargin
            val chartHeight = height - bottomMargin

            // Draw Y Grid lines
            val gridLinesCount = 4
            val gridSpacing = chartHeight / (gridLinesCount + 1)
            for (i in 1..gridLinesCount) {
                val y = i * gridSpacing
                drawLine(
                    color = BorderColor.copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val count = visibleCandlesCount.toInt().coerceAtMost(candles.size)
            val startIdx = maxOf(0, candles.size - count - scrollOffset.toInt())
            val endIdx = (startIdx + count).coerceAtMost(candles.size)

            val visibleCandles = candles.subList(startIdx, endIdx)
            if (visibleCandles.isEmpty()) return@Canvas

            val maxPrice = visibleCandles.maxOf { it.high }
            val minPrice = visibleCandles.minOf { it.low }
            val priceRange = if (maxPrice == minPrice) 1.0 else maxPrice - minPrice

            val paddedMinPrice = minPrice - (priceRange * 0.05)
            val paddedMaxPrice = maxPrice + (priceRange * 0.05)
            val paddedPriceRange = paddedMaxPrice - paddedMinPrice

            val candleWidth = chartWidth / visibleCandles.size
            val spaceBetween = candleWidth * 0.15f
            val bodyWidth = candleWidth - spaceBetween

            // Draw Y axis labels
            for (i in 0..gridLinesCount) {
                val y = i * (chartHeight / gridLinesCount)
                val price = paddedMaxPrice - (i * (paddedPriceRange / gridLinesCount))
                val priceStr = String.format("%.1f", price)
                
                drawText(
                    textMeasurer = textMeasurer,
                    text = priceStr,
                    topLeft = androidx.compose.ui.geometry.Offset(chartWidth + 4.dp.toPx(), y - 8.dp.toPx()),
                    style = androidx.compose.ui.text.TextStyle(
                        color = TextGray,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            // Draw Candlesticks & Volume Bars
            val maxVolume = visibleCandles.maxOf { it.volume }.coerceAtLeast(1.0)
            val volumeSectionHeight = chartHeight * 0.20f

            visibleCandles.forEachIndexed { index, candle ->
                val x = index * candleWidth + spaceBetween / 2f
                val isBullish = candle.close >= candle.open
                val color = if (isBullish) TextGreen else TextRed

                val yOpen = chartHeight * (1f - ((candle.open - paddedMinPrice) / paddedPriceRange).toFloat())
                val yClose = chartHeight * (1f - ((candle.close - paddedMinPrice) / paddedPriceRange).toFloat())
                val yHigh = chartHeight * (1f - ((candle.high - paddedMinPrice) / paddedPriceRange).toFloat())
                val yLow = chartHeight * (1f - ((candle.low - paddedMinPrice) / paddedPriceRange).toFloat())

                // Wick
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(x + bodyWidth / 2f, yHigh),
                    end = androidx.compose.ui.geometry.Offset(x + bodyWidth / 2f, yLow),
                    strokeWidth = 1.2.dp.toPx()
                )

                // Body
                val top = minOf(yOpen, yClose)
                val bottom = maxOf(yOpen, yClose)
                val rectHeight = maxOf(1f, bottom - top)

                drawRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(x, top),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, rectHeight)
                )

                // Volume Bar
                val volHeight = (candle.volume / maxVolume).toFloat() * volumeSectionHeight
                val volTop = chartHeight - volHeight
                drawRect(
                    color = color.copy(alpha = 0.25f),
                    topLeft = androidx.compose.ui.geometry.Offset(x, volTop),
                    size = androidx.compose.ui.geometry.Size(bodyWidth, volHeight)
                )
            }

            // Live price line & marker
            if (latestPrice >= paddedMinPrice && latestPrice <= paddedMaxPrice) {
                val yLatest = chartHeight * (1f - ((latestPrice - paddedMinPrice) / paddedPriceRange).toFloat())

                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = androidx.compose.ui.geometry.Offset(0f, yLatest),
                    end = androidx.compose.ui.geometry.Offset(chartWidth, yLatest),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                val labelHeight = 16.dp.toPx()
                val labelWidth = rightMargin - 4.dp.toPx()
                val labelTop = yLatest - labelHeight / 2f

                drawRoundRect(
                    color = Color.Black,
                    topLeft = androidx.compose.ui.geometry.Offset(chartWidth + 2.dp.toPx(), labelTop),
                    size = androidx.compose.ui.geometry.Size(labelWidth, labelHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                drawRoundRect(
                    color = EmeraldNeon,
                    topLeft = androidx.compose.ui.geometry.Offset(chartWidth + 2.dp.toPx(), labelTop),
                    size = androidx.compose.ui.geometry.Size(labelWidth, labelHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx())
                )

                val priceStr = String.format("%.1f", latestPrice)
                drawText(
                    textMeasurer = textMeasurer,
                    text = priceStr,
                    topLeft = androidx.compose.ui.geometry.Offset(chartWidth + 5.dp.toPx(), yLatest - 6.dp.toPx()),
                    style = androidx.compose.ui.text.TextStyle(
                        color = EmeraldNeon,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

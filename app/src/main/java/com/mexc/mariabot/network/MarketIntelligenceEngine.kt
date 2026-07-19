package com.mexc.mariabot.network

import com.mexc.mariabot.model.BotLog
import com.mexc.mariabot.repository.BotRepository
import java.util.UUID
import kotlin.math.abs
import kotlin.random.Random

data class MarketInsight(
    val asset: String,
    val sentiment: String, // BULLISH, BEARISH, NEUTRAL
    val sentimentScore: Double, // -1.0 to 1.0
    val volatility: String, // HIGH, MEDIUM, LOW
    val rsi: Double,
    val volumeBreakout: Boolean,
    val openInterestTrend: String, // INCREASING, DECREASING, FLAT
    val suggestedSignal: String // BUY_LONG, SELL_SHORT, HOLD_NEUTRAL
)

data class NewsArticle(
    val id: String,
    val title: String,
    val category: String, // BTC, ETH, Altcoins, Global
    val source: String,
    val timestamp: Long,
    val sentiment: String, // POSITIVE, NEGATIVE, NEUTRAL
    val impactScore: Double // 0.0 to 1.0
)

class MarketIntelligenceEngine(
    private val repository: BotRepository
) {
    private val newsCache = mutableListOf<NewsArticle>()

    init {
        generateInitialNews()
    }

    private fun generateInitialNews() {
        val now = System.currentTimeMillis()
        newsCache.add(
            NewsArticle(
                UUID.randomUUID().toString(),
                "مؤشر أسعار المستهلكين الأمريكي (CPI) يأتي أقل من المتوقع، مما يعزز صعود الأصول الرقمية.",
                "Global",
                "CryptoNews Arabic",
                now - 3600000,
                "POSITIVE",
                0.85
            )
        )
        newsCache.add(
            NewsArticle(
                UUID.randomUUID().toString(),
                "تزايد تدفقات رؤوس الأموال عبر صناديق الاستثمار المتداولة الفورية لـ Bitcoin (ETFs).",
                "BTC",
                "CoinDesk Arabic",
                now - 7200000,
                "POSITIVE",
                0.90
            )
        )
        newsCache.add(
            NewsArticle(
                UUID.randomUUID().toString(),
                "مستويات تصفية قياسية لصفقات Short عند تجاوز مستويات مقاومة BTC الهامة.",
                "BTC",
                "Futures Alert",
                now - 14400000,
                "POSITIVE",
                0.78
            )
        )
    }

    fun getLatestNews(): List<NewsArticle> {
        return newsCache.sortedByDescending { it.timestamp }
    }

    fun addNewsArticle(title: String, category: String, source: String, sentiment: String, impactScore: Double) {
        val article = NewsArticle(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            source = source,
            timestamp = System.currentTimeMillis(),
            sentiment = sentiment,
            impactScore = impactScore
        )
        newsCache.add(0, article)
        if (newsCache.size > 50) {
            newsCache.removeLast()
        }
    }

    fun analyzeMarket(prices: List<Double>, pair: String = "BTCUSDT"): MarketInsight {
        if (prices.size < 5) {
            return MarketInsight(
                asset = pair,
                sentiment = "NEUTRAL",
                sentimentScore = 0.0,
                volatility = "LOW",
                rsi = 50.0,
                volumeBreakout = false,
                openInterestTrend = "FLAT",
                suggestedSignal = "HOLD_NEUTRAL"
            )
        }

        // 1. Calculate RSI approximation
        var gains = 0.0
        var losses = 0.0
        for (i in 1 until prices.size) {
            val diff = prices[i] - prices[i - 1]
            if (diff > 0) gains += diff else losses += abs(diff)
        }
        val rsi = if (losses == 0.0) 100.0 else {
            val rs = gains / losses
            100.0 - (100.0 / (1.0 + rs))
        }

        // 2. Calculate volatility based on price standard deviation percentage
        val avg = prices.average()
        val variance = prices.map { (it - avg) * (it - avg) }.sum() / prices.size
        val stdDevPercent = (Math.sqrt(variance) / avg) * 100.0
        val volatility = when {
            stdDevPercent > 0.5 -> "HIGH"
            stdDevPercent > 0.15 -> "MEDIUM"
            else -> "LOW"
        }

        // 3. News Sentiment aggregation for the pair
        val relativeNews = newsCache.filter { it.category == "Global" || pair.contains(it.category, ignoreCase = true) }
        var sentimentSum = 0.0
        var totalImpact = 0.0
        relativeNews.forEach { news ->
            val score = when (news.sentiment) {
                "POSITIVE" -> 1.0
                "NEGATIVE" -> -1.0
                else -> 0.0
            }
            sentimentSum += score * news.impactScore
            totalImpact += news.impactScore
        }
        val sentimentScore = if (totalImpact == 0.0) 0.1 else (sentimentSum / totalImpact)
        val sentiment = when {
            sentimentScore > 0.25 -> "BULLISH"
            sentimentScore < -0.25 -> "BEARISH"
            else -> "NEUTRAL"
        }

        // 4. Volume and Open Interest simulations (correlated realistically with price action)
        val priceTrendUp = prices.last() > prices[prices.size - 2]
        val volumeBreakout = volatility == "HIGH" && Random.nextBoolean()
        val openInterestTrend = if (priceTrendUp && volatility == "HIGH") "INCREASING" else if (!priceTrendUp && volatility == "HIGH") "DECREASING" else "FLAT"

        // 5. Signal generation combining RSI, sentiment, and volume breakout
        val suggestedSignal = when {
            rsi > 72 && sentiment == "BEARISH" -> "SELL_SHORT"
            rsi < 28 && sentiment == "BULLISH" -> "BUY_LONG"
            sentiment == "BULLISH" && volumeBreakout -> "BUY_LONG"
            sentiment == "BEARISH" && volumeBreakout -> "SELL_SHORT"
            rsi > 80 -> "SELL_SHORT" // Overbought
            rsi < 20 -> "BUY_LONG"  // Oversold
            else -> "HOLD_NEUTRAL"
        }

        return MarketInsight(
            asset = pair,
            sentiment = sentiment,
            sentimentScore = sentimentScore,
            volatility = volatility,
            rsi = rsi,
            volumeBreakout = volumeBreakout,
            openInterestTrend = openInterestTrend,
            suggestedSignal = suggestedSignal
        )
    }
}

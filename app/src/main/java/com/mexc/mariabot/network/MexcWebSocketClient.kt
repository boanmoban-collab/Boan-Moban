package com.mexc.mariabot.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import java.util.concurrent.TimeUnit

class MexcWebSocketClient(
    private val onPriceUpdate: (Double) -> Unit,
    private val onLog: (String, String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private var isClosed = false

    fun connect() {
        if (isClosed) return
        
        // Official MEXC Futures public WebSocket endpoint
        val request = Request.Builder()
            .url("wss://contract.mexc.com/edge/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onLog("SUCCESS", "🔌 تم الاتصال بقنوات بث أسعار MEXC WebSocket المباشرة!")
                
                // Subscribe to BTC_USDT contract ticker stream
                val subscribeMsg = """
                    {
                        "method": "sub.ticker",
                        "param": {
                            "symbol": "BTC_USDT"
                        }
                    }
                """.trimIndent()
                webSocket.send(subscribeMsg)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson(text, JsonObject::class.java)
                    if (json.has("channel") && json.get("channel").asString == "push.ticker") {
                        val dataObj = json.getAsJsonObject("data")
                        if (dataObj != null && dataObj.has("lastPrice")) {
                            val lastPrice = dataObj.get("lastPrice").asDouble
                            if (lastPrice > 0.0) {
                                onPriceUpdate(lastPrice)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Graceful catch for unexpected frame formats
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                onLog("WARNING", "⚠️ جاري إغلاق اتصال WebSocket: $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isClosed) {
                    onLog("WARNING", "🔌 انقطع اتصال WebSocket. إعادة المحاولة تلقائياً...")
                    // Auto-reconnect after 5 seconds
                    client.dispatcher.executorService.execute {
                        try { Thread.sleep(5000) } catch (e: Exception) {}
                        connect()
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onLog("ERROR", "❌ فشل اتصال WebSocket: ${t.localizedMessage}. تفعيل المحاكي الاحتياطي تلقائياً.")
                if (!isClosed) {
                    // Try to reconnect after 10 seconds
                    client.dispatcher.executorService.execute {
                        try { Thread.sleep(10000) } catch (e: Exception) {}
                        connect()
                    }
                }
            }
        })
    }

    fun close() {
        isClosed = true
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}

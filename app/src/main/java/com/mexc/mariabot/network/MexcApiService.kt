package com.mexc.mariabot.network

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface MexcApiService {

    @GET("api/v3/time")
    fun getServerTime(): Call<ServerTimeResponse>

    @GET("api/v3/klines")
    fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 100
    ): Call<List<List<Any>>>

    @GET("api/v3/account")
    fun getSpotAccount(
        @Header("X-MEXC-APIKEY") apiKey: String,
        @QueryMap params: Map<String, String>
    ): Call<SpotAccountResponse>

    @GET("api/v1/private/account/assets")
    fun getFuturesAssets(
        @Header("X-MEXC-APIKEY") apiKey: String,
        @QueryMap params: Map<String, String>
    ): Call<FuturesAssetsResponse>

    @GET("api/v1/private/position/open_positions")
    fun getFuturesPositions(
        @Header("X-MEXC-APIKEY") apiKey: String,
        @QueryMap params: Map<String, String>
    ): Call<FuturesPositionsResponse>

    @POST("api/v1/private/order/submit")
    fun placeFuturesOrder(
        @Header("X-MEXC-APIKEY") apiKey: String,
        @QueryMap params: Map<String, String>
    ): Call<FuturesOrderResponse>
}

// Data Models for MEXC API Responses
data class SpotAccountResponse(
    @SerializedName("canTrade") val canTrade: Boolean,
    @SerializedName("balances") val balances: List<SpotAssetBalance>
)

data class SpotAssetBalance(
    @SerializedName("asset") val asset: String,
    @SerializedName("free") val free: String,
    @SerializedName("locked") val locked: String
)

data class FuturesAssetsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: List<FuturesAssetData>?
)

data class FuturesAssetData(
    @SerializedName("currency") val currency: String,
    @SerializedName("availableBalance") val availableBalance: Double,
    @SerializedName("bonus") val bonus: Double,
    @SerializedName("positionMargin") val positionMargin: Double
)

data class FuturesPositionsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: List<PositionData>?
)

data class PositionData(
    @SerializedName("positionId") val positionId: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("positionType") val positionType: Int, // 1 for long, 2 for short
    @SerializedName("openPrice") val openPrice: Double,
    @SerializedName("fairPrice") val fairPrice: Double,
    @SerializedName("vol") val vol: Double,
    @SerializedName("leverage") val leverage: Int,
    @SerializedName("realisedPnL") val realisedPnL: Double,
    @SerializedName("unrealisedPnL") val unrealisedPnL: Double
)

data class FuturesOrderResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("code") val code: Int,
    @SerializedName("data") val data: OrderResultData?
)

data class OrderResultData(
    @SerializedName("orderId") val orderId: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("price") val price: Double,
    @SerializedName("vol") val vol: Double
)

data class ServerTimeResponse(
    @SerializedName("serverTime") val serverTime: Long
)

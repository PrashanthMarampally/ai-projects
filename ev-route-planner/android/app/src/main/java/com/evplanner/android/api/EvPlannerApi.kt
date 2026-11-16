package com.evplanner.android.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface EvPlannerApi {

    @GET("api/vehicles")
    suspend fun getVehicles(): List<Vehicle>

    @POST("api/journeys/plan")
    suspend fun planJourney(@Body request: JourneyRequest): JourneyPlan

    companion object {
        // 10.0.2.2 is the Android emulator's alias for the host machine's localhost
        private const val BASE_URL = "http://10.0.2.2:8080/"

        fun create(): EvPlannerApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EvPlannerApi::class.java)
        }
    }
}

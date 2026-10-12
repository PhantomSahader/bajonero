package com.streetfood.radar.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente Singleton de Retrofit configurado para conectar con Django.
 * NOTA: En el emulador de Android Studio, '10.0.2.2' apunta al 'localhost' de tu PC Windows.
 */
object RetrofitClient {

    // IP de tu PC en la red Wi-Fi local para conectar desde tu celular físico
    private const val BASE_URL = "http://192.168.1.15:8000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: StreetFoodApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StreetFoodApiService::class.java)
    }
}

package com.infinitybutterfly.infiflyrecipe.utils

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Imports for your interfaces (Ensure these paths match your actual package)
import com.infinitybutterfly.infiflyrecipe.KtorBackendApi
import com.infinitybutterfly.infiflyrecipe.MealDbApi
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object RetrofitClient {

//    USING C++ NDK LIBRARY TO SECURE THE API MORE

    init {
        System.loadLibrary("infiflyrecipe")
    }
    private external fun getMealDbKey(): String
    private external fun getKtorKey(): String

//    private val MEALDB_BASE_URL = "${getMealDbKey()}/"
    private val MEALDB_BASE_URL by lazy {getMealDbKey()}
    private val KTOR_BASE_URL by lazy {getKtorKey()}
//    private val KTOR_BASE_URL = "${getKtorKey()}/"

//    DIRECTLY CALLING YOUR API KEYS WILL MAKE IT EASIER FOR THE HACKER TO FIND IT.
//    SAVE THEM USING C++ NDK

//    // --- 1. DEFINE YOUR BASE URLS ---
//
//    // The public MealDB API
//    private const val MEALDB_BASE_URL = "https://www.themealdb.com/api/json/v1/1/"
//
//    // Your Custom Ktor Server URL
//    // NOTE: Use "http://10.0.2.2:8080/" if testing on the Android Emulator!
//    // ("localhost" or "127.0.0.1" won't work on the emulator)
//    private const val KTOR_BASE_URL = "YOUR_OWN_CRETED_SERVER_API_LINK"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS) // Wait 90 seconds to connect
        .readTimeout(90, TimeUnit.SECONDS)    // Wait 90 seconds for data
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()


    // --- 2. BUILD THE RETROFIT ENGINES ---

    // Engine for MealDB
    private val mealDbRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(MEALDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Engine for Ktor Backend
    private val ktorRetrofit by lazy {
        Retrofit.Builder()
            .baseUrl(KTOR_BASE_URL)
            .client(okHttpClient) // <--- Add this line
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    // --- 3. EXPOSE YOUR INTERFACES ---

    // 🔴 APIs that talk to your Ktor Server:
    val myBackendApi: KtorBackendApi by lazy {
        ktorRetrofit.create(KtorBackendApi::class.java)
    }

    val profileApi: KtorBackendApi by lazy {
        ktorRetrofit.create(KtorBackendApi::class.java)
    }

    // 🔵 APIs that talk to MealDB:
    val detailsApi: MealDbApi by lazy {
        mealDbRetrofit.create(MealDbApi::class.java)
    }

    val discoveryApi: MealDbApi by lazy {
        mealDbRetrofit.create(MealDbApi::class.java)
    }

    val forYouApi: MealDbApi by lazy {
        mealDbRetrofit.create(MealDbApi::class.java)
    }

    val popularApi: MealDbApi by lazy {
        mealDbRetrofit.create(MealDbApi::class.java)
    }

    // --- SEARCH API FOR BOTH DATABASE ---

    // Use this for anything involving your custom server
    val ktorApi: KtorBackendApi by lazy {
        ktorRetrofit.create(KtorBackendApi::class.java)
    }

    // Use this for anything involving the public MealDB
    val mealDbApi: MealDbApi by lazy {
        mealDbRetrofit.create(MealDbApi::class.java)
    }

//    val searchApi: MealDbApi by lazy {
//        mealDbRetrofit.create(MealDbApi::class.java)
//        ktorRetrofit.create(KtorBackendApi::class.java)
//    }
}

package cgg.gov.`in`.task.network

import cgg.gov.`in`.task.model.CompaniesRes
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface ServiceCalls {
    object Factory {
        fun create(): ServiceCalls {
            val okHttpClient: OkHttpClient = OkHttpClient.Builder()
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .connectTimeout(120, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(VIRTUO_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            return retrofit.create(ServiceCalls::class.java)
        }
    }

    @get:GET("getallcompanies/10/20")
    var allCompanies: Call<List<CompaniesRes>>

    companion object {
        const val VIRTUO_BASE_URL = "http://dev.meetworks.in:9000/companies/"
    }
}
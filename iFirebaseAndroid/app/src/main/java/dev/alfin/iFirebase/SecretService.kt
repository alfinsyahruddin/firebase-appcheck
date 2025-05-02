package dev.alfin.iFirebase

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header

object HttpClient {
    private const val BASE_URL = "http://192.168.31.251:8000"

    private val instance: Retrofit by lazy {
        val clientBuilder: OkHttpClient.Builder =
            OkHttpClient().newBuilder()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(clientBuilder.build())
            .build()
    }

    val secretService: SecretService by lazy {
        instance.create(SecretService::class.java)
    }
}

interface SecretService {
    @GET("/secret")
    fun getSecret(
        @Header("X-Firebase-AppCheck") appCheckToken: String,
    ): Call<SecretResponse>
}

data class SecretResponse(@SerializedName("secret") val secret: String)


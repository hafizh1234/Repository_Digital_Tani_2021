package id.ac.ipb.mobile.digitani.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClientChatNew {

    companion object {

        const val URL = "URL_Digitani"
        var chatRoomApiClient: ApiInterface = getRetrofitChatRoom().create(ApiInterface::class.java)
        fun getRetrofitChatRoom(): Retrofit {
            //val httpLogingInterceptor = HttpLoggingInterceptor()

            val retrofit: Retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(URL)
                .build()
            return retrofit
        }


        fun getRetrofit(): Retrofit {
            val httpLogingInterceptor = HttpLoggingInterceptor()
            httpLogingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            val okHttpClient: OkHttpClient =
                OkHttpClient.Builder().addInterceptor(httpLogingInterceptor).build()
            val retrofit: Retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(URL)
                .client(okHttpClient)
                .build()
            return retrofit

        }

        var userApiClientLogin: ApiInterface = getRetrofit().create(ApiInterface::class.java)
    }


}

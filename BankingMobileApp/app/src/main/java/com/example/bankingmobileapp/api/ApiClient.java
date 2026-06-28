package com.example.bankingmobileapp.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static BankingApi api;

    private ApiClient() {
    }

    public static BankingApi getApi() {
        if (api == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // BASIC keeps request diagnostics without writing passwords or banking payloads to Logcat.
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankingApi.class);
        }
        return api;
    }
}

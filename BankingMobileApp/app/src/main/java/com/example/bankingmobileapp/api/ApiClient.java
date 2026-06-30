package com.example.bankingmobileapp.api;

import android.content.Context;

import com.example.bankingmobileapp.AppSession;
import com.example.bankingmobileapp.model.AuthResponse;
import com.example.bankingmobileapp.model.RefreshTokenRequest;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class ApiClient {
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static final String OTP_BASE_URL = BASE_URL;
    private static BankingApi api;
    private static BankingApi publicApi;
    private static BankingApi otpApi;
    private static Context appContext;

    private ApiClient() {
    }

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static BankingApi getApi() {
        if (api == null) {
            if (appContext == null) throw new IllegalStateException("ApiClient is not initialized");
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // BASIC keeps request diagnostics without writing passwords or banking payloads to Logcat.
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        String token = AppSession.getAuthToken(appContext);
                        if (!token.isEmpty() && request.header("Authorization") == null) {
                            request = request.newBuilder().header("Authorization", "Bearer " + token).build();
                        }
                        return chain.proceed(request);
                    })
                    .authenticator(new TokenAuthenticator())
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
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

    public static BankingApi getOtpApi() {
        if (otpApi == null) {
            otpApi = new Retrofit.Builder()
                    .baseUrl(OTP_BASE_URL)
                    .client(new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(60, TimeUnit.SECONDS)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankingApi.class);
        }
        return otpApi;
    }

    private static BankingApi getPublicApi() {
        if (publicApi == null) {
            publicApi = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankingApi.class);
        }
        return publicApi;
    }

    private static final class TokenAuthenticator implements Authenticator {
        @Override
        public Request authenticate(Route route, Response response) throws IOException {
            if (responseCount(response) >= 2 || response.request().url().encodedPath().contains("/auth/")) {
                return null;
            }
            synchronized (ApiClient.class) {
                String latestToken = AppSession.getAuthToken(appContext);
                String sentHeader = response.request().header("Authorization");
                if (!latestToken.isEmpty() && sentHeader != null
                        && !sentHeader.equals("Bearer " + latestToken)) {
                    return response.request().newBuilder()
                            .header("Authorization", "Bearer " + latestToken).build();
                }
                String refreshToken = AppSession.getRefreshToken(appContext);
                if (refreshToken.isEmpty()) return null;
                retrofit2.Response<AuthResponse> refreshed = getPublicApi()
                        .refresh(new RefreshTokenRequest(refreshToken)).execute();
                if (!refreshed.isSuccessful() || refreshed.body() == null) {
                    AppSession.clearLoginState(appContext);
                    return null;
                }
                AppSession.saveAuth(appContext, refreshed.body());
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + refreshed.body().accessToken).build();
            }
        }

        private int responseCount(Response response) {
            int count = 1;
            while ((response = response.priorResponse()) != null) count++;
            return count;
        }
    }
}

package com.example.bankingmobileapp.api;

import android.content.Context;
import android.util.Log;

import com.example.bankingmobileapp.AppSession;
import com.example.bankingmobileapp.model.AuthResponse;
import com.example.bankingmobileapp.model.RefreshTokenRequest;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public final class ApiClient {
    private static final String TAG = "BankingApi";
    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static final String AUTH_BASE_URL = "http://10.0.2.2:8082/";
    private static final long HTTP_TIMEOUT_SECONDS = 60L;
    private static BankingApi api;
    private static BankingApi authApi;
    private static Context appContext;

    private ApiClient() {
    }

    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    public static BankingApi getApi() {
        if (api == null) {
            if (appContext == null) throw new IllegalStateException("ApiClient is not initialized");
            api = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(buildClient(true))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankingApi.class);
        }
        return api;
    }

    /** Public auth client: never attaches a bearer token and never invokes token refresh. */
    public static BankingApi getAuthApi() {
        if (authApi == null) {
            authApi = new Retrofit.Builder()
                    .baseUrl(AUTH_BASE_URL)
                    .client(buildClient(false))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(BankingApi.class);
        }
        return authApi;
    }

    /** Kept for existing call sites; email OTP is part of the public auth API. */
    public static BankingApi getOtpApi() {
        return getAuthApi();
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }

    public static String getAuthBaseUrl() {
        return AUTH_BASE_URL;
    }

    private static OkHttpClient buildClient(boolean authenticated) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(
                message -> Log.d(TAG, message));
        // BASIC prints method, URL, status and duration without logging raw request bodies/passwords.
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new ApiDebugInterceptor())
                .addInterceptor(logging)
                .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (authenticated) {
            builder.addInterceptor(chain -> {
                        Request request = chain.request();
                        String token = AppSession.getAuthToken(appContext);
                        if (!token.isEmpty() && request.header("Authorization") == null) {
                            request = request.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    .authenticator(new TokenAuthenticator());
        }
        return builder.build();
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
                retrofit2.Response<AuthResponse> refreshed = getAuthApi()
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

    private static final class ApiDebugInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startedAt = System.nanoTime();
            Log.d(TAG, "Request " + request.method() + " " + request.url());
            try {
                Response response = chain.proceed(request);
                long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
                Log.d(TAG, "Response HTTP " + response.code() + " " + request.method()
                        + " " + request.url() + " (" + elapsedMillis + " ms)");
                return response;
            } catch (IOException exception) {
                Log.e(TAG, "Network failure " + request.method() + " " + request.url()
                        + ": " + exception.getMessage());
                throw exception;
            }
        }
    }
}

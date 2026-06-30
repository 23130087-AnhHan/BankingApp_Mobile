package com.example.bankingmobileapp.api;

import com.example.bankingmobileapp.model.AccountRequest;
import com.example.bankingmobileapp.model.AccountResponse;
import com.example.bankingmobileapp.model.AccountStatusRequest;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.CreateUserRequest;
import com.example.bankingmobileapp.model.FundTransferRequest;
import com.example.bankingmobileapp.model.FundTransferResponse;
import com.example.bankingmobileapp.model.TransactionResponse;
import com.example.bankingmobileapp.model.TransactionRequest;
import com.example.bankingmobileapp.model.AuthResponse;
import com.example.bankingmobileapp.model.LoginRequest;
import com.example.bankingmobileapp.model.RefreshTokenRequest;
import com.example.bankingmobileapp.model.ForgotPasswordRequest;
import com.example.bankingmobileapp.model.ResendEmailOtpRequest;
import com.example.bankingmobileapp.model.VerifyEmailOtpRequest;
import com.example.bankingmobileapp.model.AvailabilityResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface BankingApi {
    @POST("api/users/auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("api/users/auth/refresh")
    Call<AuthResponse> refresh(@Body RefreshTokenRequest body);

    @POST("api/users/auth/logout")
    Call<Void> logout(@Body RefreshTokenRequest body);

    @POST("api/users/auth/forgot-password")
    Call<Void> forgotPassword(@Body ForgotPasswordRequest body);

    @POST("api/users/register")
    Call<ApiResponse> register(@Body CreateUserRequest body);

    @POST("api/users/auth/verify-email-otp")
    Call<ApiResponse> verifyEmailOtp(@Body VerifyEmailOtpRequest body);

    @POST("api/users/auth/resend-email-otp")
    Call<ApiResponse> resendEmailOtp(@Body ResendEmailOtpRequest body);

    @GET("api/users/auth/check-email")
    Call<AvailabilityResponse> checkEmail(@Query("email") String email);

    @GET("api/users/auth/check-phone")
    Call<AvailabilityResponse> checkPhone(@Query("phone") String phone);

    @POST("accounts")
    Call<ApiResponse> createAccount(@Body AccountRequest body);

    @GET("accounts/balance")
    Call<String> getBalance(@Query("accountNumber") String accountNumber);

    @GET("accounts/{userId}")
    Call<AccountResponse> getAccountByUserId(@Path("userId") long userId);

    @PATCH("accounts")
    Call<ApiResponse> activateAccount(@Query("accountNumber") String accountNumber, @Body AccountStatusRequest body);

    @GET("transactions")
    Call<List<TransactionResponse>> getTransactions(@Query("accountId") String accountId);

    @POST("transactions")
    Call<ApiResponse> createTransaction(@Body TransactionRequest body);

    @POST("fund-transfers")
    Call<FundTransferResponse> transfer(@Body FundTransferRequest body);
}

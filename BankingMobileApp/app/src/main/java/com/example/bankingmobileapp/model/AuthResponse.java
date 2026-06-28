package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("accessToken") public String accessToken;
    @SerializedName("refreshToken") public String refreshToken;
    @SerializedName("expiresIn") public Long expiresIn;
    @SerializedName("tokenType") public String tokenType;
    @SerializedName("userId") public Long userId;
    @SerializedName("email") public String email;
    @SerializedName("displayName") public String displayName;
    @SerializedName("status") public String status;
}

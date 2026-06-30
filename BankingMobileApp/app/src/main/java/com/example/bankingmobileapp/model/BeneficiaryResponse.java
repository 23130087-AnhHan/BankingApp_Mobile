package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class BeneficiaryResponse {
    @SerializedName("id")
    public Long id;
    @SerializedName("userId")
    public Long userId;
    @SerializedName("bankName")
    public String bankName;
    @SerializedName("accountNumber")
    public String accountNumber;
    @SerializedName("accountHolderName")
    public String accountHolderName;
    @SerializedName("nickname")
    public String nickname;
    @SerializedName("createdAt")
    public String createdAt;
}

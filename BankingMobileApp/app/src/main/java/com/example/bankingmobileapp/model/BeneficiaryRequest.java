package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class BeneficiaryRequest {
    @SerializedName("bankName")
    public String bankName;
    @SerializedName("accountNumber")
    public String accountNumber;
    @SerializedName("accountHolderName")
    public String accountHolderName;
    @SerializedName("nickname")
    public String nickname;

    public BeneficiaryRequest(String bankName, String accountNumber, String accountHolderName, String nickname) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.nickname = nickname;
    }
}

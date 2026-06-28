package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AccountStatusRequest {
    @SerializedName("accountStatus")
    public String accountStatus;

    public AccountStatusRequest(String accountStatus) {
        this.accountStatus = accountStatus;
    }
}

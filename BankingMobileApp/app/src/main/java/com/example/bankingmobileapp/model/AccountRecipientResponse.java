package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AccountRecipientResponse {
    @SerializedName("bankName")
    public String bankName;
    @SerializedName("accountNumber")
    public String accountNumber;
    @SerializedName("accountHolderName")
    public String accountHolderName;
    @SerializedName("accountType")
    public String accountType;
    @SerializedName("accountStatus")
    public String accountStatus;
}

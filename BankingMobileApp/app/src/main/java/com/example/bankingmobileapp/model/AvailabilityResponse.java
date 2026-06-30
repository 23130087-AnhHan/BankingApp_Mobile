package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class AvailabilityResponse {
    @SerializedName("exists")
    public boolean exists;

    @SerializedName("message")
    public String message;
}

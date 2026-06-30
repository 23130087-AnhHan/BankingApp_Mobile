package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("id")
    public Long id;
    @SerializedName("userId")
    public Long userId;
    @SerializedName("title")
    public String title;
    @SerializedName("message")
    public String message;
    @SerializedName("type")
    public String type;
    @SerializedName("referenceId")
    public String referenceId;
    @SerializedName("read")
    public Boolean read;
    @SerializedName("createdAt")
    public String createdAt;
}

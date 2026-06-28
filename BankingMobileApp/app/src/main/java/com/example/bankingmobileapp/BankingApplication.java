package com.example.bankingmobileapp;

import android.app.Application;

import com.example.bankingmobileapp.api.ApiClient;

public class BankingApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
    }
}

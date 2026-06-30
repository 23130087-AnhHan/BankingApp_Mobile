package com.example.bankingmobileapp.model;

import com.google.gson.annotations.SerializedName;

public class CreateUserRequest {
    @SerializedName("firstName")
    public String firstName;
    @SerializedName("lastName")
    public String lastName;
    @SerializedName("contactNumber")
    public String contactNumber;
    @SerializedName("emailId")
    public String emailId;
    @SerializedName("password")
    public String password;

    public CreateUserRequest(String firstName, String lastName, String contactNumber, String emailId, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.emailId = emailId;
        this.password = password;
    }
}

package com.example.bankingmobileapp.model;

public class CreateUserRequest {
    public String firstName;
    public String lastName;
    public String contactNumber;
    public String emailId;
    public String password;

    public CreateUserRequest(String firstName, String lastName, String contactNumber, String emailId, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.emailId = emailId;
        this.password = password;
    }
}

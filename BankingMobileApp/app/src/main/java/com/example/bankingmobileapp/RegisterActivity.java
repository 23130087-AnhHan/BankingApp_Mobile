package com.example.bankingmobileapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.model.CreateUserRequest;

public class RegisterActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        EditText phoneInput = findViewById(R.id.phoneInput);
        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        TextView resultText = findViewById(R.id.resultText);

        firstNameInput.setText("Nguyen");
        lastNameInput.setText("Van A");
        phoneInput.setText("0909123456");
        emailInput.setText("demo@gmail.com");
        passwordInput.setText("123456");

        findViewById(R.id.registerButton).setOnClickListener(v -> {
            CreateUserRequest request = new CreateUserRequest(
                    Ui.text(firstNameInput),
                    Ui.text(lastNameInput),
                    Ui.text(phoneInput),
                    Ui.text(emailInput),
                    Ui.text(passwordInput)
            );
            Ui.runCall("Create customer", resultText, ApiClient.getApi().register(request));
        });
    }
}

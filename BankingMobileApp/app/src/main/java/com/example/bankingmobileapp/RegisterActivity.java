package com.example.bankingmobileapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bankingmobileapp.api.ApiClient;
import com.example.bankingmobileapp.api.ApiErrorUtils;
import com.example.bankingmobileapp.model.ApiResponse;
import com.example.bankingmobileapp.model.AvailabilityResponse;
import com.example.bankingmobileapp.model.CreateUserRequest;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends Activity {
    private static final String TAG = "RegisterActivity";
    private static final long AVAILABILITY_DEBOUNCE_MILLIS = 500L;

    private Button registerButton;
    private TextView resultText;
    private EditText emailInput;
    private EditText phoneInput;
    private TextView emailErrorText;
    private TextView phoneErrorText;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable emailCheckTask;
    private Runnable phoneCheckTask;
    private Call<AvailabilityResponse> emailCheckCall;
    private Call<AvailabilityResponse> phoneCheckCall;
    private boolean emailExists;
    private boolean phoneExists;
    private boolean emailCheckPending;
    private boolean phoneCheckPending;
    private boolean registerLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText firstNameInput = findViewById(R.id.firstNameInput);
        EditText lastNameInput = findViewById(R.id.lastNameInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        EditText confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        registerButton = findViewById(R.id.registerButton);
        resultText = findViewById(R.id.resultText);
        emailErrorText = findViewById(R.id.emailErrorText);
        phoneErrorText = findViewById(R.id.phoneErrorText);

        Ui.configurePasswordVisibility(passwordInput);
        Ui.configurePasswordVisibility(confirmPasswordInput);
        emailInput.addTextChangedListener(afterTextChanged(this::scheduleEmailCheck));
        phoneInput.addTextChangedListener(afterTextChanged(this::schedulePhoneCheck));

        registerButton.setOnClickListener(v -> {
            String firstName = Ui.text(firstNameInput);
            String lastName = Ui.text(lastNameInput);
            String phone = Ui.text(phoneInput);
            String email = Ui.text(emailInput).toLowerCase(Locale.ROOT);
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();

            if (!validateName(firstNameInput, firstName, "Tên")
                    || !validateName(lastNameInput, lastName, "Họ")
                    || !validatePhone(phoneInput, phone)
                    || !validateEmail(emailInput, email)
                    || !validatePassword(passwordInput, password)
                    || !validateConfirmPassword(confirmPasswordInput, password, confirmPassword)) {
                return;
            }
            if (emailExists) {
                showInlineError(emailErrorText, "Email đã được sử dụng");
                emailInput.requestFocus();
                return;
            }
            if (phoneExists) {
                showInlineError(phoneErrorText, "Số điện thoại đã được sử dụng");
                phoneInput.requestFocus();
                return;
            }
            if (emailCheckPending || phoneCheckPending) {
                showMessage("Vui lòng chờ kiểm tra email và số điện thoại hoàn tất.");
                return;
            }

            CreateUserRequest request = new CreateUserRequest(
                    firstName,
                    lastName,
                    phone,
                    email,
                    password
            );
            register(request, email);
        });

        findViewById(R.id.loginButton).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        cancelEmailCheck();
        cancelPhoneCheck();
        super.onDestroy();
    }

    private void scheduleEmailCheck() {
        cancelEmailCheck();
        emailExists = false;
        clearInlineError(emailErrorText);
        emailInput.setError(null);

        String email = currentEmail();
        if (email.isEmpty()) {
            emailCheckPending = false;
            updateRegisterButtonState();
            return;
        }

        emailCheckPending = true;
        updateRegisterButtonState();
        emailCheckTask = () -> {
            if (!email.equals(currentEmail())) {
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailCheckPending = false;
                showInlineError(emailErrorText, "Email không đúng định dạng");
                updateRegisterButtonState();
                return;
            }

            emailCheckCall = ApiClient.getApi().checkEmail(email);
            emailCheckCall.enqueue(new Callback<AvailabilityResponse>() {
                @Override
                public void onResponse(
                        Call<AvailabilityResponse> call,
                        Response<AvailabilityResponse> response) {
                    if (call.isCanceled() || !email.equals(currentEmail())) {
                        return;
                    }
                    emailCheckPending = false;
                    if (!response.isSuccessful() || response.body() == null) {
                        showMessage(ApiErrorUtils.httpError(
                                TAG, response, "Không thể kiểm tra email lúc này."));
                        updateRegisterButtonState();
                        return;
                    }
                    emailExists = response.body().exists;
                    if (emailExists) {
                        showInlineError(emailErrorText, "Email đã được sử dụng");
                    } else {
                        clearInlineError(emailErrorText);
                    }
                    updateRegisterButtonState();
                }

                @Override
                public void onFailure(Call<AvailabilityResponse> call, Throwable throwable) {
                    if (call.isCanceled() || !email.equals(currentEmail())) {
                        return;
                    }
                    emailCheckPending = false;
                    showMessage(ApiErrorUtils.networkError(TAG, throwable));
                    updateRegisterButtonState();
                }
            });
        };
        debounceHandler.postDelayed(emailCheckTask, AVAILABILITY_DEBOUNCE_MILLIS);
    }

    private void schedulePhoneCheck() {
        cancelPhoneCheck();
        phoneExists = false;
        clearInlineError(phoneErrorText);
        phoneInput.setError(null);

        String phone = currentPhone();
        if (phone.isEmpty()) {
            phoneCheckPending = false;
            updateRegisterButtonState();
            return;
        }

        phoneCheckPending = true;
        updateRegisterButtonState();
        phoneCheckTask = () -> {
            if (!phone.equals(currentPhone())) {
                return;
            }
            if (!phone.matches("[0-9]+")) {
                phoneCheckPending = false;
                showInlineError(phoneErrorText, "Số điện thoại chỉ được chứa chữ số");
                updateRegisterButtonState();
                return;
            }
            if (!phone.matches("0[0-9]{9}")) {
                phoneCheckPending = false;
                showInlineError(
                        phoneErrorText,
                        "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số");
                updateRegisterButtonState();
                return;
            }

            phoneCheckCall = ApiClient.getApi().checkPhone(phone);
            phoneCheckCall.enqueue(new Callback<AvailabilityResponse>() {
                @Override
                public void onResponse(
                        Call<AvailabilityResponse> call,
                        Response<AvailabilityResponse> response) {
                    if (call.isCanceled() || !phone.equals(currentPhone())) {
                        return;
                    }
                    phoneCheckPending = false;
                    if (!response.isSuccessful() || response.body() == null) {
                        showMessage(ApiErrorUtils.httpError(
                                TAG, response, "Không thể kiểm tra số điện thoại lúc này."));
                        updateRegisterButtonState();
                        return;
                    }
                    phoneExists = response.body().exists;
                    if (phoneExists) {
                        showInlineError(phoneErrorText, "Số điện thoại đã được sử dụng");
                    } else {
                        clearInlineError(phoneErrorText);
                    }
                    updateRegisterButtonState();
                }

                @Override
                public void onFailure(Call<AvailabilityResponse> call, Throwable throwable) {
                    if (call.isCanceled() || !phone.equals(currentPhone())) {
                        return;
                    }
                    phoneCheckPending = false;
                    showMessage(ApiErrorUtils.networkError(TAG, throwable));
                    updateRegisterButtonState();
                }
            });
        };
        debounceHandler.postDelayed(phoneCheckTask, AVAILABILITY_DEBOUNCE_MILLIS);
    }

    private TextWatcher afterTextChanged(Runnable action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                action.run();
            }
        };
    }

    private String currentEmail() {
        return Ui.text(emailInput).toLowerCase(Locale.ROOT);
    }

    private String currentPhone() {
        return Ui.text(phoneInput);
    }

    private void cancelEmailCheck() {
        if (emailCheckTask != null) {
            debounceHandler.removeCallbacks(emailCheckTask);
            emailCheckTask = null;
        }
        if (emailCheckCall != null) {
            emailCheckCall.cancel();
            emailCheckCall = null;
        }
    }

    private void cancelPhoneCheck() {
        if (phoneCheckTask != null) {
            debounceHandler.removeCallbacks(phoneCheckTask);
            phoneCheckTask = null;
        }
        if (phoneCheckCall != null) {
            phoneCheckCall.cancel();
            phoneCheckCall = null;
        }
    }

    private void showInlineError(TextView errorView, String message) {
        errorView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    private void clearInlineError(TextView errorView) {
        errorView.setText("");
        errorView.setVisibility(View.GONE);
    }

    private boolean validateName(EditText input, String value, String fieldName) {
        if (value.isEmpty()) {
            input.setError(fieldName + " không được để trống");
            input.requestFocus();
            return false;
        }
        if (value.length() < 2
                || !value.matches(".*\\p{L}.*")
                || !value.matches("[\\p{L} .'-]+")) {
            input.setError("Họ tên không hợp lệ");
            input.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validatePhone(EditText input, String phone) {
        if (phone.isEmpty()) {
            input.setError("Số điện thoại không được để trống");
            input.requestFocus();
            return false;
        }
        if (!phone.matches("[0-9]+")) {
            input.setError("Số điện thoại chỉ được chứa chữ số");
            input.requestFocus();
            return false;
        }
        if (!phone.matches("0[0-9]{9}")) {
            input.setError("Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số");
            input.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateEmail(EditText input, String email) {
        if (email.isEmpty()) {
            input.setError("Email không được để trống");
            input.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            input.setError("Email không đúng định dạng");
            input.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validatePassword(EditText input, String password) {
        if (password.isEmpty()) {
            input.setError("Mật khẩu không được để trống");
            input.requestFocus();
            return false;
        }
        if (password.length() < 8) {
            input.setError("Mật khẩu phải có ít nhất 8 ký tự");
            input.requestFocus();
            return false;
        }
        if (password.matches(".*\\s.*")) {
            input.setError("Mật khẩu không được chứa khoảng trắng");
            input.requestFocus();
            return false;
        }
        if (!password.matches(".*[A-Z].*")
                || !password.matches(".*[a-z].*")
                || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9\\s].*")) {
            input.setError("Mật khẩu phải gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            input.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateConfirmPassword(
            EditText input, String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            input.setError("Vui lòng nhập lại mật khẩu");
            input.requestFocus();
            return false;
        }
        if (!confirmPassword.equals(password)) {
            input.setError("Mật khẩu nhập lại không khớp");
            input.requestFocus();
            return false;
        }
        return true;
    }

    private void register(CreateUserRequest request, String email) {
        setLoading(true);
        showMessage("Đang tạo hồ sơ khách hàng...");
        ApiClient.getApi().register(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showMessage(ApiErrorUtils.httpError(
                            TAG,
                            response,
                            "Không thể tạo hồ sơ lúc này. Vui lòng thử lại."
                    ));
                    return;
                }

                AppSession.clearLoginState(RegisterActivity.this);
                AppSession.clearAccount(RegisterActivity.this);
                AppSession.saveUserEmail(RegisterActivity.this, email);
                Intent intent = new Intent(RegisterActivity.this, VerifyOtpActivity.class);
                intent.putExtra(VerifyOtpActivity.EXTRA_EMAIL, email);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                setLoading(false);
                showMessage(ApiErrorUtils.networkError(TAG, throwable));
            }
        });
    }

    private void setLoading(boolean loading) {
        registerLoading = loading;
        updateRegisterButtonState();
        registerButton.setText(loading ? "Đang tạo hồ sơ..." : "Tạo hồ sơ");
    }

    private void updateRegisterButtonState() {
        registerButton.setEnabled(
                !registerLoading
                        && !emailCheckPending
                        && !phoneCheckPending
                        && !emailExists
                        && !phoneExists);
    }

    private void showMessage(String message) {
        resultText.setVisibility(View.VISIBLE);
        resultText.setText(message);
    }
}

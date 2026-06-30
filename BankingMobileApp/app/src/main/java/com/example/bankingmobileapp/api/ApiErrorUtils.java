package com.example.bankingmobileapp.api;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Locale;

import retrofit2.Response;

public final class ApiErrorUtils {
    private ApiErrorUtils() {
    }

    public static String httpError(String tag, Response<?> response, String fallback) {
        String rawBody = readErrorBody(response);
        String serverMessage = extractMessage(rawBody);
        Log.e(tag, "HTTP " + response.code() + " " + response.message() + " body=" + rawBody);

        if (!serverMessage.isEmpty()) {
            String classified = classifyServerMessage(serverMessage);
            return classified.isEmpty() ? serverMessage : classified;
        }

        switch (response.code()) {
            case 400:
                return "Dữ liệu gửi lên không hợp lệ. Vui lòng kiểm tra lại.";
            case 401:
            case 403:
                return "Bạn không có quyền thực hiện thao tác này.";
            case 404:
                return "Không tìm thấy tài khoản hoặc dữ liệu yêu cầu.";
            case 406:
                return "Giao dịch không đáp ứng điều kiện xử lý.";
            case 409:
                return "Dữ liệu đã tồn tại hoặc đang bị trùng.";
            case 502:
            case 503:
            case 504:
                return "Dịch vụ ngân hàng đang tạm thời gián đoạn. Vui lòng thử lại.";
            default:
                return response.code() >= 500
                        ? firstNonEmpty(fallback, "Máy chủ đang gặp lỗi. Vui lòng thử lại sau.")
                        : firstNonEmpty(fallback, "Không thể xử lý yêu cầu.");
        }
    }

    public static String networkError(String tag, Throwable throwable) {
        Log.e(tag, "Network request failed", throwable);
        if (throwable instanceof SocketTimeoutException) {
            return "Kết nối quá thời gian chờ. Vui lòng thử lại.";
        }
        if (throwable instanceof IOException) {
            return "Không kết nối được server. Hãy kiểm tra backend và mạng của emulator.";
        }
        return "Ứng dụng chưa thể xử lý phản hồi từ server. Vui lòng thử lại.";
    }

    private static String readErrorBody(Response<?> response) {
        if (response.errorBody() == null) {
            return "";
        }
        try {
            return response.errorBody().string();
        } catch (IOException exception) {
            Log.e("ApiErrorUtils", "Cannot read error body", exception);
            return "";
        }
    }

    private static String extractMessage(String rawBody) {
        if (rawBody == null || rawBody.trim().isEmpty()) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(rawBody);
            if (!element.isJsonObject()) {
                return "";
            }
            JsonObject object = element.getAsJsonObject();
            String[] keys = {"errorMessage", "message", "responseMessage", "error"};
            for (String key : keys) {
                JsonElement value = object.get(key);
                if (value != null && !value.isJsonNull() && value.isJsonPrimitive()) {
                    String message = value.getAsString().trim();
                    if (!message.isEmpty()) {
                        return message;
                    }
                }
            }
        } catch (RuntimeException exception) {
            Log.e("ApiErrorUtils", "Cannot parse error body: " + rawBody, exception);
        }
        return "";
    }

    private static String classifyServerMessage(String serverMessage) {
        String normalized = serverMessage.toLowerCase(Locale.ROOT);
        if (normalized.contains("insufficient")
                || normalized.contains("minimum balance")
                || normalized.contains("amount is not available")
                || normalized.contains("số dư")) {
            return "Số dư không đủ để thực hiện giao dịch.";
        }
        if (normalized.contains("source account is not active")
                || normalized.contains("account is status is :pending")
                || normalized.contains("status is pending")
                || normalized.contains("inactive")
                || normalized.contains("closed")) {
            return "Tài khoản nguồn chưa hoạt động hoặc đã đóng.";
        }
        if (normalized.contains("destination account is not active")) {
            return "Tài khoản nhận chưa hoạt động hoặc đã đóng.";
        }
        if (normalized.contains("not found") || normalized.contains("not on the server")) {
            return "Không tìm thấy tài khoản hoặc dữ liệu yêu cầu.";
        }
        if (normalized.contains("same")
                || normalized.contains("must be different")
                || normalized.contains("greater than zero")
                || normalized.contains("missing required")) {
            return "Thông tin chuyển tiền không hợp lệ. Vui lòng kiểm tra lại.";
        }
        if (normalized.contains("already exists") || normalized.contains("already registered")) {
            return "Thông tin này đã được đăng ký trước đó.";
        }
        return "";
    }

    private static String firstNonEmpty(String preferred, String fallback) {
        return preferred == null || preferred.trim().isEmpty() ? fallback : preferred;
    }
}

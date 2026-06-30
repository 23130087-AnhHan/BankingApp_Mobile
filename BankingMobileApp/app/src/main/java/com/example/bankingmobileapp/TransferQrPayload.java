package com.example.bankingmobileapp;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

public final class TransferQrPayload {
    public static final String TYPE = "NLU_BANK_TRANSFER";
    public static final String BANK_NAME = "NLU Banking";

    public final String accountNumber;
    public final String accountHolderName;
    public final BigDecimal amount;
    public final String note;

    private TransferQrPayload(String accountNumber, String accountHolderName, BigDecimal amount, String note) {
        this.accountNumber = normalize(accountNumber);
        this.accountHolderName = normalize(accountHolderName);
        this.amount = amount;
        this.note = normalize(note);
    }

    public static TransferQrPayload create(
            String accountNumber,
            String accountHolderName,
            BigDecimal amount,
            String note) {
        return new TransferQrPayload(accountNumber, accountHolderName, amount, note);
    }

    public String toJson() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("type", TYPE);
        payload.put("bankName", BANK_NAME);
        payload.put("accountNumber", accountNumber);
        if (!accountHolderName.isEmpty()) {
            payload.put("accountHolderName", accountHolderName);
        }
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            payload.put("amount", amount.toPlainString());
        }
        if (!note.isEmpty()) {
            payload.put("note", note);
        }
        return payload.toString();
    }

    public static TransferQrPayload parse(String rawValue) throws JSONException {
        String raw = normalize(rawValue);
        if (raw.isEmpty()) {
            throw new JSONException("QR trống");
        }
        if (!raw.startsWith("{")) {
            throw new JSONException("QR không đúng định dạng NLU Banking");
        }

        JSONObject payload = new JSONObject(raw);
        String type = payload.optString("type", "");
        if (!TYPE.equals(type)) {
            throw new JSONException("QR không thuộc NLU Banking");
        }

        String accountNumber = normalize(payload.optString("accountNumber", ""));
        if (accountNumber.isEmpty()) {
            throw new JSONException("QR thiếu số tài khoản nhận");
        }

        BigDecimal amount = null;
        String rawAmount = normalize(payload.optString("amount", ""));
        if (!rawAmount.isEmpty()) {
            amount = new BigDecimal(rawAmount);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new JSONException("Số tiền trong QR không hợp lệ");
            }
        }

        return create(
                accountNumber,
                payload.optString("accountHolderName", ""),
                amount,
                payload.optString("note", ""));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

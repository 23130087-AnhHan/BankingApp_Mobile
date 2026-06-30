package org.training.fundtransfer.exception;

public class GlobalException extends RuntimeException {

    private final String errorCode;

    public GlobalException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

package org.training.fundtransfer.exception;

public class InsufficientBalance extends GlobalException{
    public InsufficientBalance(String message, String errorCode) {
        super(message, errorCode);
    }
}

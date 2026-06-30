package org.training.fundtransfer.exception;

public class AccountUpdateException extends GlobalException{
    public AccountUpdateException(String message, String errorCode) {
        super(message, errorCode);
    }
}

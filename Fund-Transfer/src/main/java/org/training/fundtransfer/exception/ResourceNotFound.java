package org.training.fundtransfer.exception;

public class ResourceNotFound extends GlobalException {
    public ResourceNotFound(String message, String errorCode) {
        super(message, errorCode);
    }
}

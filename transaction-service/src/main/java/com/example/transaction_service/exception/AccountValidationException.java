package com.example.transaction_service.exception;


public class AccountValidationException extends RuntimeException {

    public AccountValidationException(String message) {
        super(message);
    }

    public AccountValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
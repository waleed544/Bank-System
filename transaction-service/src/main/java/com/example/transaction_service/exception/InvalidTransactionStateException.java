package com.example.transaction_service.exception;

public class InvalidTransactionStateException extends RuntimeException {

    public InvalidTransactionStateException(String message) {
        super(message);
    }

}
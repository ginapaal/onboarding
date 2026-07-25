package com.example.onboarding.domain.exception;

public class InvalidCompanyStateException extends RuntimeException {

    public InvalidCompanyStateException(String message) {
        super(message);
    }
}

package com.example.onboarding.domain.exception;


public class MaxRetriesExceededException extends RuntimeException {

    public MaxRetriesExceededException(int maxAttempts) {
        super("Exceeded max payment attempts (" + maxAttempts + ")");
    }

}

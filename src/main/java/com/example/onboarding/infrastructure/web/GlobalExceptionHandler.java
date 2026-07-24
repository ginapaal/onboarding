package com.example.onboarding.infrastructure.web;

import com.example.onboarding.domain.exception.CompanyNotFoundException;
import com.example.onboarding.domain.exception.MaxRetriesExceededException;
import com.example.onboarding.domain.exception.PaymentGatewayException;
import com.example.onboarding.domain.exception.PaymentGatewayUnavailableException;
import com.example.onboarding.domain.exception.SessionNotFoundException;
import com.example.onboarding.infrastructure.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SessionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleSessionNotFound(SessionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(PaymentGatewayUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGatewayUnavailable(PaymentGatewayUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Payment service unavailable. Please try again later."));
    }

    @ExceptionHandler(PaymentGatewayException.class)
    public ResponseEntity<ErrorResponse> handlePaymentGatewayError(PaymentGatewayException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred. Please contact support."));
    }

    @ExceptionHandler(MaxRetriesExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxRetriesExceeded(MaxRetriesExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErrorResponse(ex.getMessage()));
    }
}

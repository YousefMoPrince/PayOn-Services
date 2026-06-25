package com.jo.payonservices.onpay.exception;

import com.jo.payonservices.onpay.dto.ThirdPartyTransactionApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ThirdPartyTransactionApiResponse<Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.error("ResponseStatusException caught: Status = {}, Reason = {}", ex.getStatusCode(), ex.getReason());

        ThirdPartyTransactionApiResponse<Object> apiResponse = new ThirdPartyTransactionApiResponse<>(
                ex.getReason(),
                null
        );

        return ResponseEntity.status(ex.getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ThirdPartyTransactionApiResponse<Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("IllegalStateException caught: {}", ex.getMessage());

        ThirdPartyTransactionApiResponse<Object> apiResponse = new ThirdPartyTransactionApiResponse<>(
                ex.getMessage(),
                null
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ThirdPartyTransactionApiResponse<Object>> handleRuntimeException(RuntimeException ex) {
        log.error("RuntimeException caught: ", ex);

        ThirdPartyTransactionApiResponse<Object> apiResponse = new ThirdPartyTransactionApiResponse<>(
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ThirdPartyTransactionApiResponse<Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception caught: ", ex);

        ThirdPartyTransactionApiResponse<Object> apiResponse = new ThirdPartyTransactionApiResponse<>(
                "Internal Server Error. Please contact support.",
                null
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
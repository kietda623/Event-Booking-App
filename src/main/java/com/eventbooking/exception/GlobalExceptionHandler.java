package com.eventbooking.exception;

import com.eventbooking.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return error(ex.getStatus(), ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiResponse.ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiResponse.ApiError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error("VALIDATION_ERROR", "Validation failed", errors));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return error(HttpStatus.CONFLICT, "EVENT_SOLD_OUT", "Not enough tickets available");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}

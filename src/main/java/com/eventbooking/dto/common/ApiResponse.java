package com.eventbooking.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String code;
    private List<ApiError> errors;

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, List.of());
    }

    public static <T> ApiResponse<T> error(String code, String message, List<ApiError> errors) {
        return new ApiResponse<>(false, message, null, code, errors == null ? List.of() : errors);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiError {
        private String field;
        private String message;
    }
}

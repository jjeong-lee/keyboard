package com.example.app.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Provides the OpenAPI common success envelope for internal API responses.
 *
 * @param <T> response payload type when an operation defines one
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, Map<String, Object> meta, T data) {

    public static ApiResponse<Void> successWithoutData() {
        return new ApiResponse<>(true, Map.of(), null);
    }
}

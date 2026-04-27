package com.ragask.ticketing.common.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified REST response wrapper.
 *
 * @param <T> response data type
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private String message;
    private T data;

    /**
     * Build a success response.
     *
     * @param data payload
     * @return result wrapper
     * @param <T> payload type
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "success", data);
    }

    /**
     * Build a failure response.
     *
     * @param code business error code
     * @param message error message
     * @return result wrapper
     * @param <T> payload type
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}


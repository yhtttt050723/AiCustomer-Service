package com.ragask.ticketing.common.error;

import com.ragask.ticketing.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Global exception mapping for REST endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle business exception to a unified Result wrapper.
     *
     * @param ex business exception
     * @return result wrapper
     */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBiz(BizException ex) {
        ErrorCode code = ex.getErrorCode() == null ? ErrorCode.BAD_REQUEST : ex.getErrorCode();
        return Result.fail(code.getCode(), ex.getMessage() == null ? code.getDefaultMessage() : ex.getMessage());
    }

    /**
     * Handle validation errors.
     *
     * @param ex validation exception
     * @return result wrapper
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getAllErrors().isEmpty()
                ? ErrorCode.BAD_REQUEST.getDefaultMessage()
                : ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        return Result.fail(ErrorCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * Handle Spring's ResponseStatusException.
     *
     * @param ex status exception
     * @return result wrapper
     */
    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleStatus(ResponseStatusException ex) {
        int code = ex.getStatusCode().value();
        String msg = ex.getReason() == null ? ErrorCode.BAD_REQUEST.getDefaultMessage() : ex.getReason();
        return Result.fail(code, msg);
    }

    /**
     * Handle any unexpected exceptions.
     *
     * @param ex exception
     * @return result wrapper
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR.getCode(), ErrorCode.INTERNAL_ERROR.getDefaultMessage());
    }
}


package com.training.atm.dto;

import java.util.function.Function;
import java.util.Objects;

public class ServiceResult<T> {
    private final boolean success;
    private final T data;
    private final String errorMessage;
    private final ErrorCode errorCode;

    public ServiceResult(boolean success, T data, String errorMessage, ErrorCode errorCode) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public static <T> ServiceResult<T> success(T data) {
        return new ServiceResult<>(true, data, null, null);
    }

    public static <T> ServiceResult<T> failure(ErrorCode code, Object... values) {
        Objects.requireNonNull(code, "error code must not be null");
        return failure(code.format(values), code);
    }

    private static <T> ServiceResult<T> failure(String message, ErrorCode code) {
        return new ServiceResult<>(false, null, message, code);
    }

    public <R> ServiceResult<R> map(Function<T, R> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return success
                ? ServiceResult.success(mapper.apply(data))
                : ServiceResult.failure(errorMessage, errorCode);
    }

    public ServiceResult<T> flatMap(Function<T, ServiceResult<T>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return success
                ? Objects.requireNonNull(mapper.apply(data), "mapper result must not be null")
                : ServiceResult.failure(errorMessage, errorCode);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return errorMessage;
    }
}

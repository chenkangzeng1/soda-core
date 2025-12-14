package com.hibuka.soda.foundation.io;

import com.hibuka.soda.foundation.error.IErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * Base result class, providing a standard response format with code, message, and data.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 **/
@Data
@EqualsAndHashCode(callSuper = false)
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Request ID, used for tracing requests.
     */
    private String requestId;

    /**
     * Response code, 200 means success, other means failure.
     */
    private Integer code = 200;

    /**
     * Response message, empty string means success, other means failure reason.
     */
    private String message = "";

    /**
     * Response data.
     */
    private T data;

    /**
     * Response success flag, true means success, false means failure.
     */
    private Boolean success = true;

    /**
     * Error data, only available when success is false.
     */
    private Map<String, Object> errorData = Collections.emptyMap();

    /**
     * Default constructor.
     */
    public Result() {
    }

    /**
     * Constructor with data.
     *
     * @param data response data
     */
    public Result(T data) {
        this.data = data;
    }

    /**
     * Constructor with code, message, data, success.
     *
     * @param code response code
     * @param message response message
     * @param data response data
     * @param success response success flag
     */
    public Result(Integer code, String message, T data, Boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    /**
     * Constructor with code, message, data, success, errorData.
     *
     * @param code response code
     * @param message response message
     * @param data response data
     * @param success response success flag
     * @param errorData error data
     */
    public Result(Integer code, String message, T data, Boolean success, Map<String, Object> errorData) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
        this.errorData = errorData;
    }

    /**
     * Constructor with code, message, data, success, errorData, requestId.
     *
     * @param code response code
     * @param message response message
     * @param data response data
     * @param success response success flag
     * @param errorData error data
     * @param requestId request ID
     */
    public Result(Integer code, String message, T data, Boolean success, Map<String, Object> errorData, String requestId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
        this.errorData = errorData;
        this.requestId = requestId;
    }

    /**
     * Returns a successful result with data.
     *
     * @param data response data
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "", data, true);
    }

    /**
     * Returns a successful result with data and message.
     *
     * @param data response data
     * @param message response message
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<>(200, message, data, true);
    }

    /**
     * Returns a successful result with data, message, and requestId.
     *
     * @param data response data
     * @param message response message
     * @param requestId request ID
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> successWithRequestId(T data, String message, String requestId) {
        Result<T> result = new Result<>(200, message, data, true);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a successful result with message, data is null.
     *
     * @param message response message
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null, true);
    }

    /**
     * Returns a successful result with no data.
     *
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "", null, true);
    }

    /**
     * Returns a successful result with message and requestId, data is null.
     *
     * @param message response message
     * @param requestId request ID
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> successWithRequestId(String message, String requestId) {
        Result<T> result = new Result<>(200, message, null, true);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a successful result with data and requestId, message is empty.
     *
     * @param data response data
     * @param requestId request ID
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> successWithRequestId(T data, String requestId) {
        Result<T> result = new Result<>(200, "", data, true);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a successful result with requestId, no data, message is empty.
     *
     * @param requestId request ID
     * @param <T> data type
     * @return successful result
     */
    public static <T> Result<T> successWithRequestId(String requestId) {
        Result<T> result = new Result<>(200, "", null, true);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a failed result with code, message, and data.
     *
     * @param code response code
     * @param message response message
     * @param data response data
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, message, data, false);
    }

    /**
     * Returns a failed result with code and message, data is null.
     *
     * @param code response code
     * @param message response message
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null, false);
    }

    /**
     * Returns a failed result with message, code is 500, data is null.
     *
     * @param message response message
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null, false);
    }

    /**
     * Returns a failed result with code, message, data, and requestId.
     *
     * @param code response code
     * @param message response message
     * @param data response data
     * @param requestId request ID
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> failWithRequestId(int code, String message, T data, String requestId) {
        Result<T> result = new Result<>(code, message, data, false);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a failed result with code, message, and requestId, data is null.
     *
     * @param code response code
     * @param message response message
     * @param requestId request ID
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> failWithRequestId(int code, String message, String requestId) {
        Result<T> result = new Result<>(code, message, null, false);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a failed result with message and requestId, code is 500, data is null.
     *
     * @param message response message
     * @param requestId request ID
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> failWithRequestId(String message, String requestId) {
        Result<T> result = new Result<>(500, message, null, false);
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a failed result with IErrorCode and data.
     *
     * @param errorCode error code
     * @param data response data
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> fail(IErrorCode errorCode, T data) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), data, false, errorCode.getErrorData());
    }

    /**
     * Returns a failed result with IErrorCode, data, and requestId.
     *
     * @param errorCode error code
     * @param data response data
     * @param requestId request ID
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> failWithRequestId(IErrorCode errorCode, T data, String requestId) {
        Result<T> result = new Result<>(errorCode.getCode(), errorCode.getMessage(), data, false, errorCode.getErrorData());
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Returns a failed result with IErrorCode, data is null.
     *
     * @param errorCode error code
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> fail(IErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, false, errorCode.getErrorData());
    }

    /**
     * Returns a failed result with IErrorCode and requestId, data is null.
     *
     * @param errorCode error code
     * @param requestId request ID
     * @param <T> data type
     * @return failed result
     */
    public static <T> Result<T> failWithRequestId(IErrorCode errorCode, String requestId) {
        Result<T> result = new Result<>(errorCode.getCode(), errorCode.getMessage(), null, false, errorCode.getErrorData());
        result.setRequestId(requestId);
        return result;
    }

    /**
     * Sets the request ID for this result.
     *
     * @param requestId the request ID to set
     * @return this result instance for method chaining
     */
    public Result<T> setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}
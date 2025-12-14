package com.hibuka.soda.foundation.error;

import java.io.Serial;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base exception class that implements the IErrorCode interface, representing a unified exception with error code, message, and error data.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 */
public class BaseException extends RuntimeException implements IErrorCode {
    @Serial
    private static final long serialVersionUID = 1L;
    private final int code;
    private final Map<String, Object> errorData;
    private final long timestamp;
    private final String className;
    private final String methodName;
    private final int lineNumber;

    /**
     * Constructs a new BaseException with the specified error code and message.
     *
     * @param code    the error code
     * @param message the error message
     */
    public BaseException(int code, String message) {
        this(code, message, null, null);
    }

    /**
     * Constructs a new BaseException with the specified error code, message, and cause.
     *
     * @param code    the error code
     * @param message the error message
     * @param cause   the cause of the exception
     */
    public BaseException(int code, String message, Throwable cause) {
        this(code, message, null, cause);
    }

    /**
     * Constructs a new BaseException with the specified error code, message, error data, and cause.
     *
     * @param code       the error code
     * @param message    the error message
     * @param errorData  the error data
     * @param cause      the cause of the exception
     */
    public BaseException(int code, String message, Map<String, Object> errorData, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.timestamp = System.currentTimeMillis();
        
        // Get caller information
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StackTraceElement caller = findCaller(stackTrace);
        this.className = caller.getClassName();
        this.methodName = caller.getMethodName();
        this.lineNumber = caller.getLineNumber();
        
        // Initialize error data with caller information
        Map<String, Object> contextData = new HashMap<>();
        contextData.put("timestamp", timestamp);
        contextData.put("className", className);
        contextData.put("methodName", methodName);
        contextData.put("lineNumber", lineNumber);
        
        // Add user-provided error data
        if (errorData != null && !errorData.isEmpty()) {
            contextData.putAll(errorData);
        }
        
        this.errorData = Collections.unmodifiableMap(contextData);
    }

    /**
     * Constructs a new BaseException with the specified error code, message, and error data.
     *
     * @param code       the error code
     * @param message    the error message
     * @param errorData  the error data
     */
    public BaseException(int code, String message, Map<String, Object> errorData) {
        this(code, message, errorData, null);
    }

    /**
     * Constructs a new BaseException with the specified error code.
     * The error message is derived from the error code using the BaseErrorCode.getMessage method.
     *
     * @param code the error code
     */
    public BaseException(int code) {
        this(code, BaseErrorCode.getMessage(code), null, null);
    }

    /**
     * Finds the caller of this exception in the stack trace.
     *
     * @param stackTrace the stack trace
     * @return the caller stack trace element
     */
    private StackTraceElement findCaller(StackTraceElement[] stackTrace) {
        for (StackTraceElement element : stackTrace) {
            if (!element.getClassName().equals(BaseException.class.getName()) &&
                !element.getClassName().equals("java.lang.Thread")) {
                return element;
            }
        }
        return stackTrace[0];
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public Map<String, Object> getErrorData() {
        return errorData;
    }

    /**
     * Gets the timestamp when the exception was created.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the name of the class where the exception was thrown.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Gets the name of the method where the exception was thrown.
     *
     * @return the method name
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Gets the line number in the source file where the exception was thrown.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Creates a new BaseException with the specified error code and message.
     *
     * @param code    the error code
     * @param message the error message
     * @return the new BaseException
     */
    public static BaseException of(int code, String message) {
        return new BaseException(code, message);
    }

    /**
     * Creates a new BaseException with the specified error code, message, and cause.
     *
     * @param code    the error code
     * @param message the error message
     * @param cause   the cause
     * @return the new BaseException
     */
    public static BaseException of(int code, String message, Throwable cause) {
        return new BaseException(code, message, cause);
    }

    /**
     * Creates a new BaseException with the specified error code, message, and error data.
     *
     * @param code       the error code
     * @param message    the error message
     * @param errorData  the error data
     * @return the new BaseException
     */
    public static BaseException of(int code, String message, Map<String, Object> errorData) {
        return new BaseException(code, message, errorData);
    }

    /**
     * Creates a new BaseException with the specified error code, message, error data, and cause.
     *
     * @param code       the error code
     * @param message    the error message
     * @param errorData  the error data
     * @param cause      the cause
     * @return the new BaseException
     */
    public static BaseException of(int code, String message, Map<String, Object> errorData, Throwable cause) {
        return new BaseException(code, message, errorData, cause);
    }

    /**
     * Creates a new BaseException with the specified error code.
     *
     * @param code the error code
     * @return the new BaseException
     */
    public static BaseException of(int code) {
        return new BaseException(code);
    }
}
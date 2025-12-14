package com.hibuka.soda.foundation.error;

import java.util.Map;

/**
 * Error code interface, defining standard methods for error handling.
 *
 * @author kangzeng.ckz
 * @since 2025/7/3
 */
public interface IErrorCode {
    
    /**
     * Gets the error code.
     *
     * @return the error code
     */
    int getCode();
    
    /**
     * Gets the error message.
     *
     * @return the error message
     */
    default String getMessage() {
        return BaseErrorCode.getMessage(this.getCode());
    }
    
    /**
     * Gets the error data.
     *
     * @return the error data, may be null
     */
    default Map<String, Object> getErrorData() {
        return null;
    }
}